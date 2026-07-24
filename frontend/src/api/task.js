import request from './index'

export function getTaskList(params) {
  return request.get('/tasks', { params })
}

export function getTaskById(id) {
  return request.get(`/tasks/${id}`)
}

export function createTask(data) {
  return request.post('/tasks', data)
}

export function updateTask(id, data) {
  return request.put(`/tasks/${id}`, data)
}

export function deleteTask(id) {
  return request.delete(`/tasks/${id}`)
}

export function cancelTask(id) {
  return request.put(`/tasks/${id}/cancel`)
}

export function getTaskLogs(id) {
  return request.get(`/tasks/${id}/logs`)
}

export function addTaskLog(id, data) {
  return request.post(`/tasks/${id}/logs`, data)
}

export function getAnalysisTypes() {
  return request.get('/analysis-types')
}
