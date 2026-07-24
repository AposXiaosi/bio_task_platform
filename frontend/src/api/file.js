import request from './index'

/**
 * 上传文件（返回文件元信息，包含 fileId）
 * @param {File} file - 要上传的文件
 * @param {Function} onProgress - 上传进度回调
 */
export function uploadFile(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000, // 上传超时设为 5 分钟
    onUploadProgress: onProgress
  })
}

/**
 * 获取任务的输入文件列表
 * @param {number|string} taskId
 */
export function getTaskFiles(taskId) {
  return request.get(`/files/tasks/${taskId}/files`)
}

/**
 * 根据文件 ID 下载文件（返回下载 URL）
 * @param {number|string} fileId
 */
export function getFileDownloadUrl(fileId) {
  return `/api/files/download/${fileId}`
}
