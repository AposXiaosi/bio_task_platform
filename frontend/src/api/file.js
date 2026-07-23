import request from './index'

export function getTaskResults(taskId) {
  return request.get(`/files/tasks/${taskId}/results`)
}

export function uploadTaskResult(taskId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/files/tasks/${taskId}/results`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getTaskResultDownloadUrl(taskId, resultId) {
  return `/api/files/tasks/${taskId}/results/${resultId}/download`
}
