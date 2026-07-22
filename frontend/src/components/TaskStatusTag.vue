<template>
  <el-tag
    :type="tagType"
    :class="['bio-status-tag', status]"
    size="default"
    effect="dark"
    round
  >
    <el-icon v-if="status === 'RUNNING'" class="bio-spinning" :size="12">
      <Loading />
    </el-icon>
    <el-icon v-else-if="status === 'COMPLETED'" :size="12" style="margin-right: 2px">
      <CircleCheck />
    </el-icon>
    <el-icon v-else-if="status === 'FAILED'" :size="12" style="margin-right: 2px">
      <CircleClose />
    </el-icon>
    {{ label }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'

const props = defineProps({
  status: {
    type: String,
    required: true,
    validator: (v) => ['PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'].includes(v)
  }
})

const statusMap = {
  PENDING: { type: 'info', label: '待执行' },
  RUNNING: { type: 'warning', label: '执行中' },
  COMPLETED: { type: 'success', label: '已完成' },
  FAILED: { type: 'danger', label: '失败' },
  CANCELLED: { type: 'info', label: '已取消' }
}

const tagType = computed(() => statusMap[props.status]?.type || 'info')
const label = computed(() => statusMap[props.status]?.label || props.status)
</script>
