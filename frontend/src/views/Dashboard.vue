<template>
  <div class="dashboard">
    <div class="page-heading-row">
      <div class="bio-page-header">
        <h1 class="bio-page-title">分析概览</h1>
        <p class="bio-page-subtitle">追踪任务状态、执行进度和最近分析结果</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="$router.push('/tasks/create')">新建分析</el-button>
    </div>

    <el-row :gutter="14" class="stat-row">
      <el-col v-for="item in statCards" :key="item.key" :xs="12" :sm="12" :md="6">
        <el-card class="bio-stat-card" shadow="never">
          <div class="stat-card-content">
            <div :class="['bio-stat-icon', item.color]"><el-icon :size="20"><component :is="item.icon" /></el-icon></div>
            <div><div class="bio-stat-count">{{ stats[item.key] ?? '-' }}</div><div class="bio-stat-label">{{ item.label }}</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="overview-grid">
      <el-card shadow="never" class="bio-detail-section recent-panel">
        <template #header>
          <div class="panel-heading"><div><strong>最近任务</strong><small>按创建时间排序</small></div><el-button link type="primary" @click="$router.push('/tasks')">查看全部 <el-icon><ArrowRight /></el-icon></el-button></div>
        </template>
        <el-table :data="recentTasks" class="bio-table" v-loading="loading" empty-text="暂无任务数据" @row-click="openTask">
          <el-table-column prop="name" label="任务" min-width="170">
            <template #default="{ row }"><div class="task-cell"><strong>{{ row.name }}</strong><span>#{{ row.id }} · {{ row.analysisTypeName }}</span></div></template>
          </el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }"><TaskStatusTag :status="row.status" /></template></el-table-column>
          <el-table-column label="创建时间" width="150"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
        </el-table>
      </el-card>

      <aside class="workflow-panel">
        <div class="workflow-heading"><el-icon><Operation /></el-icon><div><strong>内置分析流程</strong><span>无需外部命令行工具</span></div></div>
        <button v-for="flow in workflows" :key="flow.name" class="workflow-item" @click="$router.push('/tasks/create')">
          <span :class="['workflow-mark', flow.color]" />
          <span><strong>{{ flow.name }}</strong><small>{{ flow.caption }}</small></span>
          <el-icon><ArrowRight /></el-icon>
        </button>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Plus } from '@element-plus/icons-vue'
import { getTaskList } from '../api/task'
import TaskStatusTag from '../components/TaskStatusTag.vue'

const router = useRouter()
const loading = ref(false)
const stats = reactive({ total: 0, running: 0, completed: 0, failed: 0 })
const recentTasks = ref([])
const statCards = [
  { key: 'total', label: '全部任务', icon: 'Files', color: 'primary' },
  { key: 'running', label: '正在执行', icon: 'Loading', color: 'warning' },
  { key: 'completed', label: '分析完成', icon: 'CircleCheck', color: 'success' },
  { key: 'failed', label: '需要处理', icon: 'Warning', color: 'danger' }
]
const workflows = [
  { name: '序列分析', caption: '比对 · 组装 · 变异检测', color: 'teal' },
  { name: '转录组分析', caption: '表达定量 · 差异表达', color: 'green' },
  { name: '功能注释', caption: '序列特征 · Motif 规则', color: 'amber' }
]
function formatTime(value) { if (!value) return '-'; return new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) }
function totalOf(result) { if (result.status !== 'fulfilled') return 0; const d = result.value; return d?.totalElements ?? d?.total ?? (Array.isArray(d) ? d.length : 0) }
function openTask(row) { router.push(`/tasks/${row.id}`) }
async function fetchData() {
  loading.value = true
  try {
    const responses = await Promise.allSettled(['', 'RUNNING', 'COMPLETED', 'FAILED'].map(status => getTaskList({ page: 0, size: status ? 1 : 6, ...(status && { status }) })))
    stats.total = totalOf(responses[0]); stats.running = totalOf(responses[1]); stats.completed = totalOf(responses[2]); stats.failed = totalOf(responses[3])
    const data = responses[0].status === 'fulfilled' ? responses[0].value : null
    recentTasks.value = Array.isArray(data) ? data.slice(0, 6) : (data?.content || data?.records || [])
  } finally { loading.value = false }
}
onMounted(fetchData)
</script>

<style scoped>
.page-heading-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }.stat-row { margin-bottom: 18px; }.stat-row .el-col { margin-bottom: 14px; }
.stat-card-content { height: 100%; display: flex; align-items: center; gap: 14px; }.overview-grid { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 18px; align-items: start; }
.panel-heading { width: 100%; display: flex; align-items: center; justify-content: space-between; }.panel-heading > div { display: flex; flex-direction: column; gap: 3px; }.panel-heading strong { font-size: 14px; }.panel-heading small { color: #8a979d; font-size: 10px; }
.task-cell { display: flex; flex-direction: column; gap: 3px; cursor: pointer; }.task-cell strong { color: #26343b; font-size: 13px; }.task-cell span { color: #849198; font-size: 10px; }
.workflow-panel { overflow: hidden; border: 1px solid var(--bio-border); border-radius: var(--bio-radius); background: #fff; }.workflow-heading { min-height: 72px; padding: 14px 16px; display: flex; align-items: center; gap: 11px; border-bottom: 1px solid var(--bio-border); }.workflow-heading > .el-icon { width: 34px; height: 34px; border-radius: 6px; background: #edf5f4; color: var(--bio-primary); }.workflow-heading div { display: flex; flex-direction: column; gap: 3px; }.workflow-heading strong { font-size: 13px; }.workflow-heading span { color: #819098; font-size: 10px; }
.workflow-item { width: 100%; min-height: 64px; padding: 10px 15px; display: grid; grid-template-columns: 8px 1fr 18px; align-items: center; gap: 11px; border: 0; border-bottom: 1px solid #edf0f1; background: #fff; text-align: left; cursor: pointer; }.workflow-item:last-child { border-bottom: 0; }.workflow-item:hover { background: #f6f9f8; }.workflow-mark { width: 7px; height: 30px; border-radius: 3px; }.workflow-mark.teal { background: #168b80; }.workflow-mark.green { background: #5b933e; }.workflow-mark.amber { background: #cf832c; }.workflow-item > span:nth-child(2) { display: flex; flex-direction: column; gap: 4px; }.workflow-item strong { color: #26343b; font-size: 12px; }.workflow-item small { color: #87949a; font-size: 10px; }.workflow-item > .el-icon { color: #a1acb1; }
@media (max-width: 1000px) { .overview-grid { grid-template-columns: 1fr; }.workflow-panel { display: grid; grid-template-columns: repeat(3, 1fr); }.workflow-heading { grid-column: 1/-1; }.workflow-item { border-bottom: 0; border-right: 1px solid #edf0f1; } }
@media (max-width: 640px) { .overview-grid { display: block; }.workflow-panel { display: block; }.workflow-item { border-right: 0; border-bottom: 1px solid #edf0f1; }.recent-panel { overflow: hidden; }.page-heading-row { align-items: center; } }
</style>
