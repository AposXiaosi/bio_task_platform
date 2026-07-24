import axios from 'axios'
import request from './index'

export function uploadInputFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

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

export async function downloadTaskResult(taskId, resultId, fallbackFileName = 'analysis-result') {
  const response = await axios.get(`/api/files/tasks/${taskId}/results/${resultId}/download`, {
    responseType: 'blob',
    timeout: 30000
  })

  const blob = response.data
  const disposition = response.headers['content-disposition']
  const fileName = parseFileNameFromDisposition(disposition) || fallbackFileName
  const blobUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(blobUrl)
}

function parseFileNameFromDisposition(disposition) {
  if (!disposition) return ''

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }

  const plainMatch = disposition.match(/filename=\"?([^"]+)\"?/i)
  return plainMatch?.[1] || ''
}
