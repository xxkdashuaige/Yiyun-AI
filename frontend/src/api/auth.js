import request from './request'

export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

export function register({ username, password, confirmPassword, nickname }) {
  return request.post('/auth/register', { username, password, confirmPassword, nickname })
}

export function getMe() {
  return request.get('/auth/me')
}

export function changePassword(oldPassword, newPassword) {
  return request.put('/auth/password', { oldPassword, newPassword })
}