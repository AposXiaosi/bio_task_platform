<template>
  <div class="task-detail" v-loading="loading">
    <div class="bio-page-header" style="display: flex; align-items: flex-start; justify-content: space-between">
      <div>
        <h1 class="bio-page-title">{{ task.name || '任务详情' }}</h1>
        <p class="bio-page-subtitle">任务 ID: {{ task.id }}</p>
      </div>
      <div style="display: flex; gap: 8px; flex-shrink: 0">
        <el-button
          v-if="task.status === 'PENDING' || task.status === 'RUNNING'"
          type="warning"
          @click="handleCancel"
        >
          <el-icon style="margin-right: 4px"><VideoPause /></el-icon>
          取消任务
        </el-button>
        <el-button type="danger" @click="handleDelete">
          <el-icon style="margin-right: 4px"><Delete /></el-icon>
          删除任务
        </el-button>
        <el-button @click="$router.push('/tasks')">
          <el-icon style="margin-right: 4px"><Back /></el-icon>
          返回列表
        </el-button>
      </div>
    </div>

    <!-- Header info card -->
    <el-card shadow="hover" class="bio-detail-section" :body-style="{ padding: '20px 24px' }">
      <div style="display: flex; align-items: center; gap: 24px; flex-wrap: wrap">
        <div style="display: flex; align-items: center; gap: 10px">
          <span style="color: #909399; font-size: 13px">状态</span>
          <TaskStatusTag :status="task.status" />
        </div>
        <div style="display: flex; align-items: center; gap: 10px">
          <span style="color: #909399; font-size: 13px">优先级</span>
          <el-tag
            :type="task.priority === 'HIGH' ? 'danger' : task.priority === 'MEDIUM' ? 'warning' : 'info'"
            size="small"
            effect="plain"
          >
            {{ priorityLabel(task.priority) }}
          </el-tag>
        </div>
        <div style="display: flex; align-items: center; gap: 10px">
          <span style="color: #909399; font-size: 13px">分析类型</span>
          <el-tag type="primary" size="small" effect="plain">{{ task.analysisTypeName || '-' }}</el-tag>
        </div>
      </div>
    </el-card>

    <!-- Info section -->
    <el-card shadow="hover" class="bio-detail-section" :body-style="{ padding: '20px 24px' }">
      <template #header>
        <span style="font-weight: 600; font-size: 14px">基本信息</span>
      </template>
      <div class="bio-info-grid">
        <div class="bio-info-item">
          <span class="bio-info-label">任务描述</span>
          <span class="bio-info-value">{{ task.description || '-' }}</span>
        </div>
        <div class="bio-info-item">
          <span class="bio-info-label">输入文件</span>
          <span class="bio-info-value" style="word-break: break-all">{{ formatFiles(task.inputFiles) }}</span>
        </div>
        <div class="bio-info-item">
          <span class="bio-info-label">输出目录</span>
          <span class="bio-info-value" style="word-break: break-all">{{ task.outputDirectory || task.outputDir || '-' }}</span>
        </div>
        <div class="bio-info-item">
          <span class="bio-info-label">创建时间</span>
          <span class="bio-info-value">{{ formatTime(task.createdAt) }}</span>
        </div>
        <div class="bio-info-item">
          <span class="bio-info-label">更新时间</span>
          <span class="bio-info-value">{{ formatTime(task.updatedAt) }}</span>
        </div>
        <div class="bio-info-item">
          <span class="bio-info-label">完成时间</span>
          <span class="bio-info-value">{{ formatTime(task.completedAt || task.finishedAt) }}</span>
        </div>
      </div>
    </el-card>

    <!-- Tabs: parameters, logs, results -->
    <el-card shadow="hover" class="bio-detail-section" :body-style="{ padding: '0' }">
      <el-tabs v-model="activeTab" style="padding: 0 20px">
        <!-- Parameters Tab -->
        <el-tab-pane label="参数" name="params">
          <div style="padding: 16px 0">
            <el-table
              v-if="paramsList.length > 0"
              :data="paramsList"
              class="bio-table"
              style="width: 100%"
              :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: 600 }"
              :show-header="false"
            >
              <el-table-column prop="key" label="参数名" width="200">
                <template #default="{ row }">
                  <span style="font-weight: 600; color: #303133">{{ row.key }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="value" label="参数值">
                <template #default="{ row }">
                  <span style="color: #606266">{{ row.value ?? '-' }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-else class="bio-empty">
              <el-icon><Document /></el-icon>
              <p>暂无参数数据</p>
            </div>
          </div>
        </el-tab-pane>

        <!-- Logs Tab -->
        <el-tab-pane label="日志" name="logs">
          <div style="padding: 16px 0">
            <div v-if="logs.length > 0" class="bio-log-viewer" ref="logViewerRef">
              <div v-for="(log, idx) in logs" :key="idx" class="bio-log-line">
                <span class="bio-log-time">{{ formatTime(log.timestamp || log.createdAt) }}</span>
                <span :class="['bio-log-level', (log.level || 'INFO').toUpperCase()]">
                  [{{ (log.level || 'INFO').toUpperCase() }}]
                </span>
                <span class="bio-log-msg">{{ log.message || log.content || '' }}</span>
              </div>
            </div>
            <div v-else class="bio-empty">
              <el-icon><Tickets /></el-icon>
              <p>暂无日志数据</p>
            </div>
          </div>
        </el-tab-pane>

        <!-- Results Tab -->
        <el-tab-pane label="结果文件" name="results">
          <div style="padding: 16px 0">
            <div style="display: flex; justify-content: flex-end; margin-bottom: 16px">
              <el-upload
                :show-file-list="false"
                :before-upload="beforeUpload"
                :http-request="handleResultUpload"
                accept=".txt,.fasta,.fa,.TXT,.FASTA,.FA"
              >
                <el-button type="primary">
                  <el-icon style="margin-right: 4px"><Upload /></el-icon>
                  上传结果文件
                </el-button>
              </el-upload>
            </div>
            <el-table
              v-if="results.length > 0"
              :data="results"
              class="bio-table"
              style="width: 100%"
              :header-cell-style="{ background: '#f5f7fa', color: '#606266', fontWeight: 600 }"
              empty-text="暂无结果文件"
            >
              <el-table-column label="文件名" min-width="220">
                <template #default="{ row }">
                  <div style="display: flex; align-items: center">
                    <span :class="['bio-result-icon', getFileIconClass(row.fileName || row.name)]">
                      <el-icon><Document /></el-icon>
                    </span>
                    <span style="color: #303133">{{ row.fileName || row.name }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="文件大小" width="120">
                <template #default="{ row }">
                  {{ formatSize(row.fileSize || row.size) }}
                </template>
              </el-table-column>
              <el-table-column label="类型" width="100">
                <template #default="{ row }">
                  {{ getFileExt(row.fileName || row.name) }}
                </template>
              </el-table-column>
              <el-table-column label="创建时间" width="180">
                <template #default="{ row }">
                  {{ formatTime(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    link
                    size="small"
                    :loading="downloadingResultId === (row.id || row.fileId)"
                    @click="downloadFile(row)"
                  >
                    <el-icon style="margin-right: 2px"><Download /></el-icon>
                    下载
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-else class="bio-empty">
              <el-icon><FolderOpened /></el-icon>
              <p>暂无结果文件</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Back, Download, VideoPause, Document, Tickets, FolderOpened, Upload } from '@element-plus/icons-vue'
import { getTaskById, deleteTask, cancelTask, getTaskLogs } from '../api/task'
import { getTaskResults, uploadTaskResult, downloadTaskResult } from '../api/file'
import TaskStatusTag from '../components/TaskStatusTag.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('params')
const logViewerRef = ref(null)
const downloadingResultId = ref(null)

const task = ref({})
const logs = ref([])
const results = ref([])

let logPollingTimer = null

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

function formatFiles(files) {
  if (!files) return '-'
  if (Array.isArray(files)) return files.join('\n')
  return String(files)
}

function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = Number(bytes)
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(i === 0 ? 0 : 1)} ${units[i]}`
}

function getFileExt(name) {
  if (!name) return '-'
  const parts = name.split('.')
  return parts.length > 1 ? `.${parts.pop()}` : '-'
}

function getFileIconClass(name) {
  if (!name) return 'default'
  const ext = getFileExt(name).toLowerCase()
  if (['.fa', '.fasta', '.fq', '.fastq', '.fna', '.faa'].includes(ext)) return 'seq'
  if (['.csv', '.tsv'].includes(ext)) return 'csv'
  if (['.txt', '.log'].includes(ext)) return 'txt'
  return 'default'
}

const paramsList = computed(() => {
  const params = task.value.parameters || task.value.params || {}
  if (!params || typeof params !== 'object') return []
  return Object.entries(params)
    .filter(([_, v]) => v !== undefined && v !== null && v !== '')
    .map(([key, value]) => ({ key, value: typeof value === 'object' ? JSON.stringify(value) : String(value) }))
})

function scrollToBottom() {
  nextTick(() => {
    if (logViewerRef.value) {
      logViewerRef.value.scrollTop = logViewerRef.value.scrollHeight
    }
  })
}

async function fetchTask() {
  const id = route.params.id
  loading.value = true
  try {
    const res = await getTaskById(id)
    task.value = res || {}
  } catch (e) {
    console.error('Fetch task error:', e)
  } finally {
    loading.value = false
  }
}

async function fetchResults() {
  const id = route.params.id
  try {
    const res = await getTaskResults(id)
    results.value = Array.isArray(res) ? res : (res?.list || res?.records || res?.content || [])
  } catch (e) {
    results.value = []
    console.error('Fetch task results error:', e)
  }
}

async function fetchLogs() {
  const id = route.params.id
  try {
    const res = await getTaskLogs(id)
    logs.value = Array.isArray(res) ? res : (res?.list || res?.records || res?.content || [])
    scrollToBottom()
  } catch {
    logs.value = []
  }
}

function startLogPolling() {
  stopLogPolling()
  logPollingTimer = setInterval(() => {
    if (task.value.status === 'RUNNING' || task.value.status === 'PENDING') {
      fetchLogs()
      fetchTask()
    }
  }, 5000)
}

function stopLogPolling() {
  if (logPollingTimer) {
    clearInterval(logPollingTimer)
    logPollingTimer = null
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm(
      `确定要取消任务「${task.value.name}」吗？`,
      '取消任务',
      { confirmButtonText: '确定取消', cancelButtonText: '返回', type: 'warning' }
    )
    await cancelTask(task.value.id)
    ElMessage.success('任务已取消')
    fetchTask()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务「${task.value.name}」吗？此操作不可恢复。`,
      '删除任务',
      { confirmButtonText: '确定删除', cancelButtonText: '返回', type: 'error', confirmButtonClass: 'el-button--danger' }
    )
    await deleteTask(task.value.id)
    ElMessage.success('任务已删除')
    router.push('/tasks')
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function downloadFile(row) {
  const id = route.params.id
  const fileId = row.id || row.fileId
  const fileName = row.fileName || row.name || 'analysis-result'
  if (!fileId) {
    ElMessage.error('结果文件信息不完整，无法下载')
    return
  }

  downloadingResultId.value = fileId
  try {
    await downloadTaskResult(id, fileId, fileName)
    ElMessage.success('开始下载结果文件')
  } catch (e) {
    console.error('Download result file error:', e)
    ElMessage.error('下载结果文件失败，请稍后重试')
  } finally {
    downloadingResultId.value = null
  }
}

function beforeUpload(file) {
  const allowedExtensions = ['.txt', '.fasta', '.fa']
  const fileName = file?.name || ''
  const extension = fileName.includes('.') ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : ''
  if (!allowedExtensions.includes(extension)) {
    ElMessage.error('文件格式不支持，仅允许上传 .txt、.fasta、.fa 文件')
    return false
  }
  return true
}

async function handleResultUpload({ file }) {
  const id = route.params.id
  try {
    await uploadTaskResult(id, file)
    ElMessage.success('结果文件上传成功')
    await fetchResults()
    await fetchTask()
  } catch (e) {
    console.error('Upload result file error:', e)
  }
}

onMounted(async () => {
  await fetchTask()
  await fetchResults()
  await fetchLogs()
  startLogPolling()
})

onUnmounted(stopLogPolling)
</script>

<style scoped>
.task-detail {
  max-width: 1100px;
}
</style>
