<template>
  <div class="task-create">
    <div class="bio-page-header">
      <h1 class="bio-page-title">新建任务</h1>
      <p class="bio-page-subtitle">创建一个新的生物信息分析任务</p>
    </div>

    <el-card shadow="hover" class="bio-form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleSubmit"
      >
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="任务名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入任务名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分析类型" prop="analysisTypeId">
              <el-select v-model="form.analysisTypeId" placeholder="请选择分析类型" style="width: 100%" @change="onTypeChange">
                <el-option
                  v-for="t in analysisTypes"
                  :key="t.id"
                  :label="t.name"
                  :value="t.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="form.priority" placeholder="请选择优先级" style="width: 100%">
                <el-option label="低" value="LOW" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="高" value="HIGH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="输入文件路径" prop="inputFiles">
              <el-input
                v-model="form.inputFiles"
                type="textarea"
                :rows="2"
                :placeholder="selectedType === 'sequence_alignment' ? '请输入待比对序列文件路径，多个文件用逗号分隔' : '请输入输入文件路径，多个文件用逗号分隔'"
              />
            </el-form-item>
            <div v-if="selectedType === 'sequence_alignment'" style="display: flex; justify-content: space-between; align-items: center; margin-top: -8px">
              <span style="font-size: 12px; color: #909399">可直接上传待比对序列文件，系统会自动回填文件路径</span>
              <el-upload
                :show-file-list="false"
                :before-upload="beforeSequenceFileUpload"
                :http-request="handleSequenceInputUpload"
                multiple
                accept=".txt,.fasta,.fa,.TXT,.FASTA,.FA"
              >
                <el-button :loading="uploadingQuery" size="small">
                  <el-icon style="margin-right: 4px"><Upload /></el-icon>
                  上传待比对文件
                </el-button>
              </el-upload>
            </div>
          </el-col>
        </el-row>

        <el-form-item label="任务描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述（可选）"
          />
        </el-form-item>

        <!-- Dynamic parameters based on analysis type -->
        <template v-if="selectedType">
          <el-divider content-position="left">
            <span style="font-weight: 600; color: #606266; font-size: 14px">分析参数 - {{ selectedTypeName }}</span>
          </el-divider>

          <!-- 序列比对 -->
          <template v-if="selectedType === 'sequence_alignment'">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="参考基因组路径" prop="params.referenceGenome">
                  <el-input v-model="form.params.referenceGenome" placeholder="/path/to/reference.fa" />
                </el-form-item>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-top: -8px">
                  <span style="font-size: 12px; color: #909399">建议上传 `.fa/.fasta/.txt` 格式参考基因组文件</span>
                  <el-upload
                    :show-file-list="false"
                    :before-upload="beforeSequenceFileUpload"
                    :http-request="handleReferenceUpload"
                    accept=".txt,.fasta,.fa,.TXT,.FASTA,.FA"
                  >
                    <el-button :loading="uploadingReference" size="small">
                      <el-icon style="margin-right: 4px"><Upload /></el-icon>
                      上传参考基因组
                    </el-button>
                  </el-upload>
                </div>
              </el-col>
              <el-col :span="6">
                <el-form-item label="匹配得分" prop="params.matchScore">
                  <el-input-number v-model="form.params.matchScore" :min="1" :max="10" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="错配罚分" prop="params.mismatchPenalty">
                  <el-input-number v-model="form.params.mismatchPenalty" :min="1" :max="10" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="空位罚分" prop="params.gapPenalty">
                  <el-input-number v-model="form.params.gapPenalty" :min="1" :max="20" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 序列组装 -->
          <template v-if="selectedType === 'sequence_assembly'">
            <el-row :gutter="24">
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
          <template v-if="selectedType === 'gene_expression'">
            <el-row :gutter="24">
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
          <template v-if="selectedType === 'diff_expression'">
            <el-row :gutter="24">
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
            <el-row :gutter="24">
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
          <template v-if="selectedType === 'variant_calling'">
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="参考基因组" prop="params.referenceGenome">
                  <el-input v-model="form.params.referenceGenome" placeholder="/path/to/reference.fa" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最低变异质量" prop="params.minVariantQuality">
                  <el-input-number v-model="form.params.minVariantQuality" :min="10" :max="100" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最低覆盖深度" prop="params.minDepth">
                  <el-input-number v-model="form.params.minDepth" :min="1" :max="200" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 功能注释 -->
          <template v-if="selectedType === 'functional_annotation'">
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="数据库路径" prop="params.databasePath">
                  <el-input v-model="form.params.databasePath" placeholder="/path/to/database" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="E-value 阈值" prop="params.eValueThreshold">
                  <el-input-number v-model="form.params.eValueThreshold" :min="-10" :max="0" :step="0.5" :precision="1" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最大命中数" prop="params.maxHits">
                  <el-input-number v-model="form.params.maxHits" :min="1" :max="1000" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>
        </template>

        <el-divider />

        <div style="display: flex; gap: 12px; justify-content: flex-end">
          <el-button @click="handleCancel" size="large">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large">
            <el-icon style="margin-right: 4px"><Check /></el-icon>
            提交任务
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Upload } from '@element-plus/icons-vue'
import { createTask, getAnalysisTypes } from '../api/task'
import { uploadInputFile } from '../api/file'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const analysisTypes = ref([])
const uploadingReference = ref(false)
const uploadingQuery = ref(false)

const form = reactive({
  name: '',
  analysisTypeId: '',
  priority: 'MEDIUM',
  description: '',
  inputFiles: '',
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
  const typeCode = t?.type || t?.code || ''
  return typeCode ? String(typeCode).trim().toLowerCase() : null
})

const selectedTypeName = computed(() => {
  const t = analysisTypes.value.find(a => a.id === form.analysisTypeId)
  return t?.name || ''
})

function onTypeChange() {
  form.params = {}
}

function sanitizeQuotedPath(value) {
  if (!value) return value
  const trimmed = String(value).trim()
  if (trimmed.length >= 2 && trimmed.startsWith('"') && trimmed.endsWith('"')) {
    return trimmed.slice(1, -1).trim()
  }
  return trimmed
}

function beforeSequenceFileUpload(file) {
  const allowedExtensions = ['.txt', '.fasta', '.fa']
  const fileName = file?.name || ''
  const extension = fileName.includes('.') ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : ''
  if (!allowedExtensions.includes(extension)) {
    ElMessage.error('文件格式不支持，仅允许上传 .txt、.fasta、.fa 文件')
    return false
  }
  return true
}

async function handleReferenceUpload({ file }) {
  uploadingReference.value = true
  try {
    const path = await uploadInputFile(file)
    form.params.referenceGenome = path
    ElMessage.success('参考基因组上传成功')
  } catch (e) {
    console.error('Upload reference genome error:', e)
  } finally {
    uploadingReference.value = false
  }
}

async function handleSequenceInputUpload({ file }) {
  uploadingQuery.value = true
  try {
    const path = await uploadInputFile(file)
    const currentPaths = (form.inputFiles || '')
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
    if (!currentPaths.includes(path)) {
      currentPaths.push(path)
    }
    form.inputFiles = currentPaths.join(',')
    ElMessage.success('待比对文件上传成功')
  } catch (e) {
    console.error('Upload sequence input file error:', e)
  } finally {
    uploadingQuery.value = false
  }
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
    inputFiles: form.inputFiles || undefined,
    parameters: Object.keys(form.params).length > 0 ? { ...form.params } : undefined
  }
  if (data.inputFiles && data.inputFiles.trim()) {
    data.inputFiles = data.inputFiles.split(',').map(f => sanitizeQuotedPath(f)).filter(Boolean).join(',')
  }
  if (data.parameters?.referenceGenome) {
    data.parameters.referenceGenome = sanitizeQuotedPath(data.parameters.referenceGenome)
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

  if (selectedType.value === 'sequence_alignment') {
    if (!form.inputFiles || !form.inputFiles.trim()) {
      ElMessage.warning('请上传或填写待比对文件路径')
      return
    }
    if (!form.params.referenceGenome || !String(form.params.referenceGenome).trim()) {
      ElMessage.warning('请上传或填写参考基因组路径')
      return
    }
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
.task-create {
  max-width: 960px;
}
</style>
