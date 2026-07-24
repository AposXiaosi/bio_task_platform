<template>
  <div class="task-create">
    <div class="bio-page-header">
      <h1 class="bio-page-title">新建任务</h1>
      <p class="bio-page-subtitle">创建一个新的生物信息分析任务</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleSubmit"
    >
      <div class="create-layout">
        <!-- ========== 左侧：表单主区域 ========== -->
        <div class="create-main">
          <!-- 基本信息卡片 -->
          <el-card shadow="hover" class="section-card">
            <div class="section-header">
              <el-icon class="section-icon"><EditPen /></el-icon>
              <span class="section-title">基本信息</span>
            </div>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="任务名称" prop="name">
                  <el-input v-model="form.name" placeholder="请输入任务名称" maxlength="100" show-word-limit />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="分析类型" prop="analysisTypeId">
                  <el-select v-model="form.analysisTypeId" placeholder="请选择分析类型" style="width: 100%" @change="onTypeChange">
                    <el-option v-for="t in analysisTypes" :key="t.id" :label="t.name" :value="t.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="优先级" prop="priority">
                  <el-radio-group v-model="form.priority">
                    <el-radio-button value="LOW">低</el-radio-button>
                    <el-radio-button value="MEDIUM">中</el-radio-button>
                    <el-radio-button value="HIGH">高</el-radio-button>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="任务描述" prop="description">
                  <el-input
                    v-model="form.description"
                    type="textarea"
                    :rows="1"
                    :autosize="{ minRows: 1, maxRows: 3 }"
                    placeholder="请输入任务描述（可选）"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-card>

          <!-- 分析参数卡片 -->
          <el-card v-if="selectedType" shadow="hover" class="section-card">
            <div class="section-header">
              <el-icon class="section-icon"><Setting /></el-icon>
              <span class="section-title">分析参数</span>
              <el-tag size="small" type="info" effect="plain" class="section-tag">{{ selectedTypeName }}</el-tag>
            </div>

            <!-- 序列比对 -->
            <template v-if="selectedType === 'SEQUENCE_ALIGNMENT'">
              <el-row :gutter="20">
                <el-col :span="24">
                  <el-form-item label="参考基因组路径" prop="params.referenceGenome">
                    <el-input v-model="form.params.referenceGenome" placeholder="/path/to/reference.fa" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="匹配得分" prop="params.matchScore">
                    <el-input-number v-model="form.params.matchScore" :min="1" :max="10" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="错配罚分" prop="params.mismatchPenalty">
                    <el-input-number v-model="form.params.mismatchPenalty" :min="1" :max="10" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="空位罚分" prop="params.gapPenalty">
                    <el-input-number v-model="form.params.gapPenalty" :min="1" :max="20" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
            </template>

            <!-- 序列组装 -->
            <template v-if="selectedType === 'SEQUENCE_ASSEMBLY'">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="读长 (bp)" prop="params.readLength">
                    <el-input-number v-model="form.params.readLength" :min="50" :max="10000" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="覆盖度" prop="params.coverage">
                    <el-input-number v-model="form.params.coverage" :min="1" :max="500" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="组装算法" prop="params.algorithm">
                    <el-select v-model="form.params.algorithm" placeholder="选择算法" style="width: 100%">
                      <el-option label="SPAdes" value="SPAdes" />
                      <el-option label="MEGAHIT" value="MEGAHIT" />
                      <el-option label="SOAPdenovo" value="SOAPdenovo" />
                      <el-option label="Velvet" value="Velvet" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>

            <!-- 基因表达分析 -->
            <template v-if="selectedType === 'GENE_EXPRESSION'">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="参考注释文件" prop="params.referenceAnnotation">
                    <el-input v-model="form.params.referenceAnnotation" placeholder="/path/to/annotation.gtf" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="标准化方法" prop="params.normalizationMethod">
                    <el-select v-model="form.params.normalizationMethod" placeholder="选择标准化方法" style="width: 100%">
                      <el-option label="TPM" value="TPM" />
                      <el-option label="FPKM" value="FPKM" />
                      <el-option label="RPKM" value="RPKM" />
                      <el-option label="Raw Counts" value="RAW" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>

            <!-- 差异表达分析 -->
            <template v-if="selectedType === 'DIFFERENTIAL_EXPRESSION'">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="对照组" prop="params.controlGroup">
                    <el-input v-model="form.params.controlGroup" placeholder="请输入对照组名称" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="处理组" prop="params.treatmentGroup">
                    <el-input v-model="form.params.treatmentGroup" placeholder="请输入处理组名称" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="P值阈值" prop="params.pValueThreshold">
                    <el-input-number v-model="form.params.pValueThreshold" :min="0.001" :max="0.1" :step="0.001" :precision="4" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="倍数变化阈值" prop="params.foldChange">
                    <el-input-number v-model="form.params.foldChange" :min="1" :max="20" :step="0.5" :precision="1" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
            </template>

            <!-- 变异检测 -->
            <template v-if="selectedType === 'VARIANT_CALLING'">
              <el-row :gutter="20">
                <el-col :span="24">
                  <el-form-item label="参考基因组" prop="params.referenceGenome">
                    <el-input v-model="form.params.referenceGenome" placeholder="/path/to/reference.fa" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="最低变异质量" prop="params.minVariantQuality">
                    <el-input-number v-model="form.params.minVariantQuality" :min="10" :max="100" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="最低覆盖深度" prop="params.minDepth">
                    <el-input-number v-model="form.params.minDepth" :min="1" :max="200" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
            </template>

            <!-- 功能注释 -->
            <template v-if="selectedType === 'FUNCTIONAL_ANNOTATION'">
              <el-row :gutter="20">
                <el-col :span="24">
                  <el-form-item label="数据库路径" prop="params.databasePath">
                    <el-input v-model="form.params.databasePath" placeholder="/path/to/database" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="E-value 阈值" prop="params.eValueThreshold">
                    <el-input-number v-model="form.params.eValueThreshold" :min="-10" :max="0" :step="0.5" :precision="1" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="最大命中数" prop="params.maxHits">
                    <el-input-number v-model="form.params.maxHits" :min="1" :max="1000" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
          </el-card>
        </div>

        <!-- ========== 右侧：文件上传 & 操作面板 ========== -->
        <div class="create-sidebar">
          <!-- 文件上传卡片 -->
          <el-card shadow="hover" class="section-card">
            <div class="section-header">
              <el-icon class="section-icon"><UploadFilled /></el-icon>
              <span class="section-title">输入文件</span>
              <el-tag v-if="uploadedFiles.length" size="small" round class="file-count-tag">{{ uploadedFiles.length }}</el-tag>
            </div>

            <el-upload
              drag
              :http-request="handleFileUpload"
              :show-file-list="false"
              :before-upload="beforeUpload"
              :disabled="uploading"
              multiple
              accept=".fa,.fasta,.fq,.fastq,.bam,.vcf,.txt,.csv,.zip,.fna,.faa,.gtf,.gff"
              class="upload-dragger"
            >
              <div class="upload-inner">
                <el-icon v-if="!uploading" class="upload-icon"><UploadFilled /></el-icon>
                <el-icon v-else class="upload-icon is-loading"><Loading /></el-icon>
                <div class="upload-text">
                  <span v-if="!uploading">将文件拖拽至此处，或 <em>点击上传</em></span>
                  <span v-else>正在上传...</span>
                </div>
              </div>
            </el-upload>
            <div class="upload-tip">支持 .fasta / .fastq / .bam / .vcf / .csv 等，单文件最大 100MB</div>

            <!-- 已上传文件列表 -->
            <div v-if="uploadedFiles.length > 0" class="uploaded-file-list">
              <TransitionGroup name="file-list">
                <div v-for="f in uploadedFiles" :key="f.fileId" class="uploaded-file-item">
                  <el-icon color="#1a73e8"><Document /></el-icon>
                  <span class="uploaded-file-name" :title="f.fileName">{{ f.fileName }}</span>
                  <span class="uploaded-file-size">{{ formatSize(f.fileSize) }}</span>
                  <el-button type="danger" link size="small" @click="handleRemoveFile(f.fileId)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </TransitionGroup>
            </div>
          </el-card>

          <!-- 操作按钮卡片 -->
          <el-card shadow="hover" class="section-card action-card">
            <div class="action-summary" v-if="form.name || selectedTypeName">
              <div class="summary-item" v-if="form.name">
                <span class="summary-label">任务</span>
                <span class="summary-value">{{ form.name }}</span>
              </div>
              <div class="summary-item" v-if="selectedTypeName">
                <span class="summary-label">类型</span>
                <span class="summary-value">{{ selectedTypeName }}</span>
              </div>
              <div class="summary-item" v-if="uploadedFiles.length">
                <span class="summary-label">文件</span>
                <span class="summary-value">{{ uploadedFiles.length }} 个</span>
              </div>
            </div>
            <div class="action-buttons">
              <el-button @click="handleCancel" size="large" class="action-btn">取消</el-button>
              <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large" class="action-btn">
                <el-icon v-if="!submitting" style="margin-right: 4px"><Check /></el-icon>
                提交任务
              </el-button>
            </div>
          </el-card>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, UploadFilled, Document, Delete, EditPen, Setting, Loading } from '@element-plus/icons-vue'
import { createTask, getAnalysisTypes } from '../api/task'
import { uploadFile } from '../api/file'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const uploading = ref(false)
const analysisTypes = ref([])
const uploadedFiles = ref([])

const form = reactive({
  name: '',
  analysisTypeId: '',
  priority: 'MEDIUM',
  description: '',
  params: {}
})

const rules = {
  name: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { min: 2, max: 100, message: '名称长度应在 2-100 个字符之间', trigger: 'blur' }
  ],
  analysisTypeId: [
    { required: true, message: '请选择分析类型', trigger: 'change' }
  ],
  priority: [
    { required: true, message: '请选择优先级', trigger: 'change' }
  ]
}

const selectedType = computed(() => {
  const t = analysisTypes.value.find(a => a.id === form.analysisTypeId)
  return t?.type || t?.code || null
})

const selectedTypeName = computed(() => {
  const t = analysisTypes.value.find(a => a.id === form.analysisTypeId)
  return t?.name || ''
})

function onTypeChange() {
  form.params = {}
}

// 文件上传相关
function beforeUpload(file) {
  const maxSize = 100 * 1024 * 1024 // 100MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }
  return true
}

async function handleFileUpload(options) {
  uploading.value = true
  try {
    const res = await uploadFile(options.file, options.onProgress)
    uploadedFiles.value.push({
      fileId: res.fileId,
      fileName: res.fileName,
      fileSize: res.fileSize,
      fileType: res.fileType
    })
    ElMessage.success(`${options.file.name} 上传成功`)
  } catch (e) {
    ElMessage.error('文件上传失败')
  } finally {
    uploading.value = false
  }
}

function handleRemoveFile(fileId) {
  uploadedFiles.value = uploadedFiles.value.filter(f => f.fileId !== fileId)
}

function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = Number(bytes)
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(i === 0 ? 0 : 1)} ${units[i]}`
}

async function fetchAnalysisTypes() {
  try {
    const res = await getAnalysisTypes()
    analysisTypes.value = Array.isArray(res) ? res : (res?.list || res?.records || [])
  } catch {
    analysisTypes.value = []
  }
}

function buildPayload() {
  const data = {
    name: form.name,
    analysisTypeId: form.analysisTypeId,
    priority: form.priority,
    description: form.description || undefined,
    fileIds: uploadedFiles.value.length > 0
      ? uploadedFiles.value.map(f => f.fileId)
      : undefined,
    parameters: Object.keys(form.params).length > 0 ? { ...form.params } : undefined
  }
  return data
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完善表单必填项')
    return
  }

  submitting.value = true
  try {
    const payload = buildPayload()
    await createTask(payload)
    ElMessage.success('任务创建成功')
    router.push('/tasks')
  } catch (e) {
    console.error('Create task error:', e)
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push('/tasks')
}

onMounted(fetchAnalysisTypes)
</script>

<style scoped>
/* ===== 双栏布局 ===== */
.create-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 20px;
  align-items: start;
}

.create-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.create-sidebar {
  position: sticky;
  top: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===== 卡片通用 ===== */
.section-card {
  border-radius: 10px;
  border: none;
}

.section-card :deep(.el-card__body) {
  padding: 24px 28px;
}

/* ===== Section Header ===== */
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #ebeef5;
}

.section-icon {
  font-size: 18px;
  color: #1a73e8;
  background: #e8f0fe;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a2138;
}

.section-tag {
  margin-left: 4px;
}

/* ===== 优先级 Radio Group ===== */
:deep(.el-radio-group) {
  width: 100%;
}

:deep(.el-radio-button) {
  flex: 1;
}

:deep(.el-radio-button .el-radio-button__inner) {
  width: 100%;
  border-radius: 0;
}

:deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 6px 0 0 6px;
}

:deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 6px 6px 0;
}

/* ===== 拖拽上传区 ===== */
.upload-dragger {
  width: 100%;
}

.upload-dragger :deep(.el-upload) {
  width: 100%;
}

.upload-dragger :deep(.el-upload-dragger) {
  border: 2px dashed #d0d7de;
  border-radius: 10px;
  padding: 28px 16px;
  transition: all 0.25s ease;
  background: #f8fafc;
}

.upload-dragger :deep(.el-upload-dragger:hover) {
  border-color: #1a73e8;
  background: #f0f6ff;
}

.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  font-size: 36px;
  color: #b0bec5;
  transition: color 0.25s;
}

.upload-dragger :deep(.el-upload-dragger:hover) .upload-icon {
  color: #1a73e8;
}

.upload-icon.is-loading {
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.upload-text {
  font-size: 13px;
  color: #606266;
}

.upload-text em {
  color: #1a73e8;
  font-style: normal;
  font-weight: 500;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.file-count-tag {
  margin-left: auto;
  background: #1a73e8;
  color: #fff;
  border: none;
}

/* ===== 已上传文件列表 ===== */
.uploaded-file-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.uploaded-file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f5f7fa;
  transition: all 0.2s;
}

.uploaded-file-item:hover {
  background: #e8f0fe;
}

.uploaded-file-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.uploaded-file-size {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

/* 文件列表动画 */
.file-list-enter-active {
  transition: all 0.3s ease;
}

.file-list-leave-active {
  transition: all 0.2s ease;
}

.file-list-enter-from {
  opacity: 0;
  transform: translateX(-16px);
}

.file-list-leave-to {
  opacity: 0;
  transform: translateX(16px);
}

/* ===== 操作面板 ===== */
.action-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.action-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-label {
  font-size: 12px;
  color: #909399;
}

.summary-value {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.action-btn {
  flex: 1;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .create-layout {
    grid-template-columns: 1fr;
  }

  .create-sidebar {
    position: static;
  }
}
</style>
