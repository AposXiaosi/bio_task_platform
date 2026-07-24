<template>
  <div class="dashboard">
    <div class="bio-page-header">
      <h1 class="bio-page-title">仪表盘</h1>
      <p class="bio-page-subtitle">生物信息分析任务概览</p>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6" v-for="item in statCards" :key="item.key">
        <el-card shadow="hover" class="bio-stat-card" :body-style="{ padding: '20px' }">
          <div style="display: flex; align-items: center; gap: 14px">
            <div :class="['bio-stat-icon', item.color]">
              <el-icon :size="24"><component :is="item.icon" /></el-icon>
            </div>
            <div>
              <div class="bio-stat-count">{{ stats[item.key] ?? '-' }}</div>
              <div class="bio-stat-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="bio-detail-section" :body-style="{ padding: '0' }">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="font-weight: 600; font-size: 15px">最近任务</span>
          <el-button type="primary" link @click="$router.push('/tasks')">查看全部</el-button>
        </div>
      </template>
      <el-table
        :data="recentTasks"
        class="bio-table"
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: 600 }"
        v-loading="loading"
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
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getTaskList } from '../api/task'
import TaskStatusTag from '../components/TaskStatusTag.vue'

const loading = ref(false)
const stats = reactive({
  total: 0,
  running: 0,
  completed: 0,
  failed: 0
})
const recentTasks = ref([])

const statCards = [
  { key: 'total', label: '总任务数', icon: 'Files', color: 'primary' },
  { key: 'running', label: '执行中', icon: 'Loading', color: 'warning' },
  { key: 'completed', label: '已完成', icon: 'CircleCheck', color: 'success' },
  { key: 'failed', label: '失败', icon: 'CircleClose', color: 'danger' }
]

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

async function fetchData() {
  loading.value = true
  try {
    const [allRes, runRes, compRes, failRes] = await Promise.allSettled([
      getTaskList({ page: 0, size: 1 }),
      getTaskList({ page: 0, size: 1, status: 'RUNNING' }),
      getTaskList({ page: 0, size: 1, status: 'COMPLETED' }),
      getTaskList({ page: 0, size: 1, status: 'FAILED' })
    ])

    const extractTotal = (res) => {
      if (res.status === 'fulfilled') {
        const d = res.value
        return d?.totalElements ?? d?.total ?? d?.totalCount ?? (Array.isArray(d) ? d.length : 0)
      }
      return 0
    }

    stats.total = extractTotal(allRes)
    stats.running = extractTotal(runRes)
    stats.completed = extractTotal(compRes)
    stats.failed = extractTotal(failRes)

    const listRes = await getTaskList({ page: 0, size: 5 })
    const list = Array.isArray(listRes) ? listRes : (listRes?.records || listRes?.content || listRes?.list || [])
    recentTasks.value = list
  } catch (e) {
    console.error('Dashboard fetch error:', e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.stat-row {
  margin-bottom: 20px;
}

.stat-row .el-col {
  margin-bottom: 8px;
}
</style>
