<template>
  <div class="task-list">
    <div class="bio-page-header">
      <h1 class="bio-page-title">任务管理</h1>
      <p class="bio-page-subtitle">管理和监控所有分析任务</p>
    </div>

    <div class="bio-filter-bar">
      <el-form :inline="true" :model="filters" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索任务名称"
            clearable
            :prefix-icon="Search"
            style="width: 200px"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px" @change="handleSearch">
            <el-option label="待执行" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="分析类型">
          <el-select v-model="filters.analysisTypeId" placeholder="全部类型" clearable style="width: 160px" @change="handleSearch">
            <el-option
              v-for="t in analysisTypes"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshRight" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card shadow="hover" :body-style="{ padding: '0' }">
      <el-table
        :data="tasks"
        class="bio-table"
        v-loading="loading"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: 600 }"
        empty-text="暂无任务数据"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="任务名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="analysisTypeName" label="分析类型" width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <TaskStatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag
              :type="row.priority === 'HIGH' ? 'danger' : row.priority === 'MEDIUM' ? 'warning' : 'info'"
              size="small"
              effect="plain"
            >
              {{ priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewTask(row)">查看</el-button>
            <el-button type="warning" link size="small" @click="editTask(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'RUNNING'"
              type="info"
              link
              size="small"
              @click="handleCancel(row)"
            >取消</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="bio-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchTasks"
          @current-change="fetchTasks"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { getTaskList, deleteTask, cancelTask, getAnalysisTypes } from '../api/task'
import TaskStatusTag from '../components/TaskStatusTag.vue'

const router = useRouter()
const loading = ref(false)
const tasks = ref([])
const analysisTypes = ref([])

const filters = reactive({
  keyword: '',
  status: '',
  analysisTypeId: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

function priorityLabel(p) {
  const map = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[p] || p || '-'
}

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

async function fetchAnalysisTypes() {
  try {
    const res = await getAnalysisTypes()
    analysisTypes.value = Array.isArray(res) ? res : (res?.list || res?.records || [])
  } catch {
    analysisTypes.value = []
  }
}

async function fetchTasks() {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size
    }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.status) params.status = filters.status
    if (filters.analysisTypeId) params.analysisTypeId = filters.analysisTypeId

    const res = await getTaskList(params)
    if (Array.isArray(res)) {
      tasks.value = res
      pagination.total = res.length
    } else {
      tasks.value = res?.records || res?.content || res?.list || res?.data || []
      pagination.total = res?.total || res?.totalCount || res?.totalElements || tasks.value.length
    }
  } catch {
    tasks.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchTasks()
}

function handleReset() {
  filters.keyword = ''
  filters.status = ''
  filters.analysisTypeId = ''
  pagination.page = 1
  fetchTasks()
}

function viewTask(row) {
  router.push(`/tasks/${row.id}`)
}

function editTask(row) {
  router.push(`/tasks/${row.id}`)
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm(
      `确定要取消任务「${row.name}」吗？`,
      '取消任务',
      { confirmButtonText: '确定取消', cancelButtonText: '返回', type: 'warning' }
    )
    await cancelTask(row.id)
    ElMessage.success('任务已取消')
    fetchTasks()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务「${row.name}」吗？此操作不可恢复。`,
      '删除任务',
      { confirmButtonText: '确定删除', cancelButtonText: '返回', type: 'error', confirmButtonClass: 'el-button--danger' }
    )
    await deleteTask(row.id)
    ElMessage.success('任务已删除')
    fetchTasks()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(() => {
  fetchAnalysisTypes()
  fetchTasks()
})
</script>

<style scoped>
.bio-filter-bar {
  margin-bottom: 16px;
}
</style>
