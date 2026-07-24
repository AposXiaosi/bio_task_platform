<template>
  <div class="task-detail" v-loading="loading">
    <div class="detail-heading">
      <div class="title-wrap">
        <el-button :icon="ArrowLeft" circle aria-label="返回任务列表" @click="$router.push('/tasks')" />
        <div><div class="title-meta">TASK #{{ task.id || '-' }}</div><h1 class="bio-page-title">{{ task.name || '任务详情' }}</h1></div>
      </div>
      <div class="detail-actions">
        <el-button v-if="task.status === 'PENDING'" type="primary" :icon="VideoPlay" :loading="executing" @click="handleExecute">执行分析</el-button>
        <el-button v-if="task.status === 'RUNNING'" type="warning" plain :icon="VideoPause" @click="handleCancel">取消</el-button>
        <el-dropdown trigger="click">
          <el-button :icon="MoreFilled" circle aria-label="更多操作" />
          <template #dropdown><el-dropdown-menu><el-dropdown-item @click="loadSolution"><el-icon><Operation /></el-icon>查看执行方案</el-dropdown-item><el-dropdown-item divided @click="handleDelete"><el-icon><Delete /></el-icon>删除任务</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </div>
    </div>

    <section class="status-band">
      <div class="status-main"><TaskStatusTag :status="task.status || 'PENDING'" /><span>{{ statusDescription }}</span></div>
      <div class="status-facts">
        <div><small>分析类型</small><strong>{{ task.analysisTypeName || '-' }}</strong></div>
        <div><small>优先级</small><strong>{{ priorityLabel(task.priority) }}</strong></div>
        <div><small>开始时间</small><strong>{{ formatTime(task.startedAt) }}</strong></div>
        <div><small>运行时长</small><strong>{{ duration }}</strong></div>
      </div>
    </section>

    <el-alert v-if="task.errorMessage" class="error-alert" type="error" :closable="false" show-icon :title="task.errorMessage" />

    <el-card shadow="never" class="bio-detail-section detail-tabs-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane name="overview"><template #label><span class="tab-label"><el-icon><DataBoard /></el-icon>概览</span></template>
          <div class="tab-content overview-content">
            <div class="section-block"><h3>任务信息</h3><div class="bio-info-grid">
              <div class="bio-info-item"><span class="bio-info-label">任务描述</span><span class="bio-info-value">{{ task.description || '未填写' }}</span></div>
              <div class="bio-info-item"><span class="bio-info-label">输入文件</span><span class="bio-info-value">{{ inputFiles.length }} 个</span></div>
              <div class="bio-info-item"><span class="bio-info-label">结果文件</span><span class="bio-info-value">{{ results.length }} 个</span></div>
              <div class="bio-info-item"><span class="bio-info-label">创建时间</span><span class="bio-info-value">{{ formatTime(task.createdAt) }}</span></div>
              <div class="bio-info-item"><span class="bio-info-label">完成时间</span><span class="bio-info-value">{{ formatTime(task.finishedAt) }}</span></div>
              <div class="bio-info-item"><span class="bio-info-label">输出目录</span><span class="bio-info-value">{{ task.outputDir || '-' }}</span></div>
            </div></div>
            <div class="section-block"><h3>分析参数</h3><div v-if="paramsList.length" class="parameter-grid"><div v-for="item in paramsList" :key="item.key"><span>{{ paramLabel(item.key) }}</span><strong>{{ item.value }}</strong></div></div><el-empty v-else :image-size="56" description="未配置额外参数" /></div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="files"><template #label><span class="tab-label"><el-icon><FolderOpened /></el-icon>输入文件 <b>{{ inputFiles.length }}</b></span></template>
          <div class="tab-content"><el-table :data="inputFiles" class="bio-table" empty-text="暂无输入文件">
            <el-table-column label="文件" min-width="240"><template #default="{ row }"><div class="file-cell"><span :class="['bio-result-icon', fileClass(row.originalName)]"><el-icon><Document /></el-icon></span><div><strong>{{ row.originalName }}</strong><small>{{ row.fileType || fileExt(row.originalName) }}</small></div></div></template></el-table-column>
            <el-table-column label="大小" width="120"><template #default="{ row }">{{ formatSize(row.fileSize) }}</template></el-table-column>
            <el-table-column label="上传时间" width="180"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
            <el-table-column width="72" align="right"><template #default="{ row }"><el-tooltip content="下载输入文件"><el-button :icon="Download" circle text @click="downloadInput(row.id)" /></el-tooltip></template></el-table-column>
          </el-table></div>
        </el-tab-pane>

        <el-tab-pane name="logs"><template #label><span class="tab-label"><el-icon><Tickets /></el-icon>运行日志 <b>{{ logs.length }}</b></span></template>
          <div class="tab-content log-tab">
            <div class="log-toolbar">
              <div class="log-filters"><el-select v-model="logLevel" size="small" style="width: 110px"><el-option label="全部级别" value="" /><el-option label="INFO" value="INFO" /><el-option label="WARN" value="WARN" /><el-option label="ERROR" value="ERROR" /></el-select><el-input v-model="logKeyword" size="small" clearable :prefix-icon="Search" placeholder="搜索日志" style="width: 210px" /></div>
              <div><span v-if="task.status === 'RUNNING'" class="live-indicator"><i />实时更新</span><el-tooltip content="刷新日志"><el-button :icon="Refresh" circle size="small" @click="fetchLogs" /></el-tooltip><el-tooltip content="下载日志"><el-button :icon="Download" circle size="small" @click="downloadLogs" /></el-tooltip></div>
            </div>
            <div v-if="filteredLogs.length" ref="logViewerRef" class="bio-log-viewer"><div v-for="log in filteredLogs" :key="log.id" class="bio-log-line"><span class="bio-log-time">{{ formatLogTime(log.createdAt) }}</span><span :class="['bio-log-level', log.level]">{{ log.level }}</span><span class="bio-log-msg">{{ log.message }}</span></div></div>
            <div v-else class="bio-empty"><el-icon><Tickets /></el-icon><p>暂无匹配日志</p></div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="results"><template #label><span class="tab-label"><el-icon><Collection /></el-icon>分析结果 <b>{{ results.length }}</b></span></template>
          <div class="tab-content"><el-table :data="results" class="bio-table" empty-text="任务完成后将在此显示结果文件">
            <el-table-column label="结果文件" min-width="250"><template #default="{ row }"><div class="file-cell"><span :class="['bio-result-icon', fileClass(row.fileName)]"><el-icon><Document /></el-icon></span><div><strong>{{ row.fileName }}</strong><small>{{ row.description || row.fileType }}</small></div></div></template></el-table-column>
            <el-table-column label="格式" width="90"><template #default="{ row }"><el-tag size="small" effect="plain">{{ row.fileType || fileExt(row.fileName) }}</el-tag></template></el-table-column>
            <el-table-column label="大小" width="110"><template #default="{ row }">{{ formatSize(row.fileSize) }}</template></el-table-column>
            <el-table-column label="生成时间" width="180"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
            <el-table-column width="72" align="right"><template #default="{ row }"><el-tooltip content="下载结果"><el-button type="primary" :icon="Download" circle plain @click="downloadResult(row.id)" /></el-tooltip></template></el-table-column>
          </el-table></div>
        </el-tab-pane>

        <el-tab-pane name="solution"><template #label><span class="tab-label"><el-icon><Operation /></el-icon>执行方案</span></template>
          <div class="tab-content"><div v-if="solution.typeCode" class="solution-view"><div class="solution-header"><div><small>REPRODUCIBLE WORKFLOW</small><h3>{{ solution.typeName }}</h3><p>{{ solution.description }}</p></div><el-tag type="success" effect="plain">内置引擎</el-tag></div><div class="workflow-steps"><div v-for="(step, index) in solution.workflow" :key="step"><span>{{ index + 1 }}</span><strong>{{ step }}</strong></div></div><div class="command-box"><span>执行标识</span><code>{{ solution.command }}</code></div></div><el-empty v-else :image-size="64" description="正在加载执行方案" /></div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Download, MoreFilled, Refresh, Search, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import { cancelTask, deleteTask, executeTask, getTaskById, getTaskLogs, getTaskSolution } from '../api/task'
import { getFileDownloadUrl, getResultDownloadUrl, getTaskFiles, getTaskResults } from '../api/file'
import TaskStatusTag from '../components/TaskStatusTag.vue'

const route = useRoute(), router = useRouter()
const task = ref({}), logs = ref([]), results = ref([]), inputFiles = ref([]), solution = ref({})
const loading = ref(false), executing = ref(false), activeTab = ref('overview'), logLevel = ref(''), logKeyword = ref(''), logViewerRef = ref(null)
let timer
const paramsList = computed(() => Object.entries(task.value.parameters || {}).map(([key, value]) => ({ key, value })))
const filteredLogs = computed(() => logs.value.filter(item => (!logLevel.value || item.level === logLevel.value) && (!logKeyword.value || item.message.toLowerCase().includes(logKeyword.value.toLowerCase()))))
const statusDescription = computed(() => ({ PENDING: '等待手动启动分析', RUNNING: '分析引擎正在处理输入数据', COMPLETED: '分析已完成，结果可以下载', FAILED: '执行失败，请查看错误和运行日志', CANCELLED: '任务已取消' }[task.value.status] || ''))
const duration = computed(() => { if (!task.value.startedAt) return '-'; const end = task.value.finishedAt ? new Date(task.value.finishedAt) : new Date(); const sec = Math.max(0, Math.floor((end - new Date(task.value.startedAt)) / 1000)); return sec < 60 ? `${sec} 秒` : `${Math.floor(sec / 60)} 分 ${sec % 60} 秒` })
function priorityLabel(v) { return ({ HIGH: '高', MEDIUM: '中', LOW: '低' }[v] || '-') }
function formatTime(v) { return v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '-' }
function formatLogTime(v) { if (!v) return '-'; const d = new Date(v); return `${d.toLocaleDateString('zh-CN')} ${d.toLocaleTimeString('zh-CN', { hour12: false })}` }
function formatSize(bytes) { if (bytes == null) return '-'; const u = ['B', 'KB', 'MB', 'GB']; let n = Number(bytes), i = 0; while (n >= 1024 && i < u.length - 1) { n /= 1024; i++ } return `${n.toFixed(i ? 1 : 0)} ${u[i]}` }
function fileExt(name = '') { return name.includes('.') ? name.split('.').pop().toUpperCase() : 'FILE' }
function fileClass(name) { const ext = fileExt(name); return ['FA','FASTA','FQ','FASTQ','VCF'].includes(ext) ? 'seq' : ['CSV','TSV','JSON'].includes(ext) ? 'csv' : ['TXT','LOG'].includes(ext) ? 'txt' : 'default' }
function paramLabel(key) { return ({ matchScore: '匹配得分', mismatchPenalty: '错配罚分', gapPenalty: '空位罚分', min_overlap: '最小重叠', coverage: '覆盖度', algorithm: '算法', normalizationMethod: '标准化方法', controlGroup: '对照组', treatmentGroup: '处理组', pValueThreshold: 'P 值阈值', foldChange: '倍数阈值', minVariantQuality: '最低质量', minDepth: '最低深度' }[key] || key) }
async function fetchTask() { const data = await getTaskById(route.params.id); task.value = data || {} }
async function fetchLogs() { try { logs.value = await getTaskLogs(route.params.id) || []; await nextTick(); if (logViewerRef.value) logViewerRef.value.scrollTop = logViewerRef.value.scrollHeight } catch { logs.value = [] } }
async function fetchFiles() { const [input, output] = await Promise.allSettled([getTaskFiles(route.params.id), getTaskResults(route.params.id)]); inputFiles.value = input.status === 'fulfilled' ? input.value || [] : []; results.value = output.status === 'fulfilled' ? output.value || [] : [] }
async function refreshAll() { await Promise.all([fetchTask(), fetchLogs(), fetchFiles()]) }
async function handleExecute() { try { await ElMessageBox.confirm('将使用上传文件启动此分析任务。', '执行分析', { confirmButtonText: '开始执行', cancelButtonText: '取消', type: 'info' }); executing.value = true; await executeTask(task.value.id); activeTab.value = 'logs'; ElMessage.success('分析任务已启动'); await refreshAll() } catch (e) { if (e !== 'cancel') console.error(e) } finally { executing.value = false } }
async function handleCancel() { try { await ElMessageBox.confirm('确定取消当前分析吗？', '取消任务', { type: 'warning' }); await cancelTask(task.value.id); ElMessage.success('任务已取消'); await refreshAll() } catch (e) { if (e !== 'cancel') console.error(e) } }
async function handleDelete() { try { await ElMessageBox.confirm(`删除任务「${task.value.name}」及其记录？`, '删除任务', { type: 'error', confirmButtonText: '删除' }); await deleteTask(task.value.id); router.push('/tasks') } catch (e) { if (e !== 'cancel') console.error(e) } }
async function loadSolution() { activeTab.value = 'solution'; solution.value = await getTaskSolution(route.params.id) || {} }
function downloadInput(id) { window.open(getFileDownloadUrl(id), '_blank') }
function downloadResult(id) { window.open(getResultDownloadUrl(id), '_blank') }
function downloadLogs() { window.open(`/api/tasks/${route.params.id}/logs/download`, '_blank') }
onMounted(async () => { loading.value = true; try { await refreshAll() } finally { loading.value = false } timer = setInterval(async () => { if (task.value.status === 'RUNNING' || task.value.status === 'PENDING') await refreshAll() }, 2000) })
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.detail-heading { margin-bottom: 18px; display: flex; align-items: center; justify-content: space-between; gap: 18px; }.title-wrap,.detail-actions { display: flex; align-items: center; gap: 12px; }.title-meta { margin-bottom: 4px; color: #87949a; font-size: 9px; font-weight: 700; letter-spacing: 1px; }
.status-band { min-height: 112px; margin-bottom: 18px; padding: 18px 22px; display: flex; align-items: center; justify-content: space-between; gap: 24px; border: 1px solid var(--bio-border); border-radius: var(--bio-radius); background: #fff; }.status-main { min-width: 190px; display: flex; flex-direction: column; align-items: flex-start; gap: 9px; }.status-main > span:last-child { color: #69777e; font-size: 11px; }.status-facts { flex: 1; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); }.status-facts > div { min-width: 0; padding: 0 20px; display: flex; flex-direction: column; gap: 5px; border-left: 1px solid #e4e9eb; }.status-facts small { color: #89969c; font-size: 10px; }.status-facts strong { overflow: hidden; color: #26343b; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.error-alert { margin-bottom: 18px; }.detail-tabs-card :deep(.el-card__body) { padding: 0; }.detail-tabs-card :deep(.el-tabs__header) { margin: 0; padding: 0 20px; }.detail-tabs-card :deep(.el-tabs__content) { min-height: 360px; }.tab-label { display: inline-flex; align-items: center; gap: 6px; }.tab-label b { min-width: 18px; padding: 1px 5px; border-radius: 8px; background: #edf1f2; color: #66767d; font-size: 9px; text-align: center; }.tab-content { padding: 20px; }.overview-content { display: grid; gap: 26px; }.section-block h3 { margin: 0 0 16px; font-size: 13px; }.parameter-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1px; overflow: hidden; border: 1px solid var(--bio-border); border-radius: 6px; background: var(--bio-border); }.parameter-grid > div { min-height: 60px; padding: 11px 14px; display: flex; flex-direction: column; gap: 5px; background: #fff; }.parameter-grid span { color: #87949b; font-size: 10px; }.parameter-grid strong { color: #2d3a40; font-size: 12px; }
.file-cell { display: flex; align-items: center; }.file-cell > div { min-width: 0; display: flex; flex-direction: column; gap: 3px; }.file-cell strong { overflow: hidden; color: #29373e; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.file-cell small { overflow: hidden; color: #87949a; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.log-toolbar { margin-bottom: 10px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }.log-filters,.log-toolbar > div:last-child { display: flex; align-items: center; gap: 8px; }.live-indicator { margin-right: 4px; color: #65747b; font-size: 10px; }.live-indicator i { width: 6px; height: 6px; margin-right: 5px; display: inline-block; border-radius: 50%; background: #28a860; box-shadow: 0 0 0 3px rgba(40,168,96,.13); }
.solution-view { display: grid; gap: 22px; }.solution-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }.solution-header small { color: #168378; font-size: 9px; font-weight: 700; letter-spacing: 1px; }.solution-header h3 { margin: 6px 0; font-size: 17px; }.solution-header p { margin: 0; color: #68777e; font-size: 12px; }.workflow-steps { display: grid; grid-template-columns: repeat(4, 1fr); border: 1px solid var(--bio-border); border-radius: 6px; }.workflow-steps > div { min-height: 76px; padding: 14px; display: flex; align-items: center; gap: 9px; border-right: 1px solid var(--bio-border); }.workflow-steps > div:last-child { border-right: 0; }.workflow-steps span { width: 22px; height: 22px; display: grid; place-items: center; border-radius: 50%; background: #e5f2f0; color: #087f73; font-size: 9px; }.workflow-steps strong { font-size: 11px; }.command-box { padding: 14px; border-radius: 6px; background: #182126; }.command-box span { display: block; margin-bottom: 7px; color: #7f929a; font-size: 9px; }.command-box code { color: #73cfc4; font-size: 12px; }
@media (max-width: 850px) { .status-band { align-items: flex-start; flex-direction: column; }.status-facts { width: 100%; grid-template-columns: repeat(2, 1fr); gap: 14px; }.status-facts > div:nth-child(3) { border-left: 0; }.workflow-steps { grid-template-columns: repeat(2, 1fr); }.workflow-steps > div:nth-child(2) { border-right: 0; }.workflow-steps > div:nth-child(-n+2) { border-bottom: 1px solid var(--bio-border); } }
@media (max-width: 640px) { .detail-heading { align-items: flex-start; }.detail-actions .el-button:not(:first-child) { display: none; }.status-facts { grid-template-columns: 1fr; }.status-facts > div { padding: 0; border-left: 0; }.parameter-grid { grid-template-columns: 1fr; }.log-toolbar { align-items: stretch; flex-direction: column; }.log-filters { flex-wrap: wrap; }.workflow-steps { grid-template-columns: 1fr; }.workflow-steps > div { border-right: 0; border-bottom: 1px solid var(--bio-border); } }
</style>
