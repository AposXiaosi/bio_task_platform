import request from './index'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function getUserInfo(userId) {
  return request.get('/user/info', { params: { userId } })
}
