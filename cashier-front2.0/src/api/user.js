import request from '@/utils/request'

// ------ 用户 ------
export const login = (data) => request.post('/users/login', data)
export const getCurrentUser = () => request.get('/users/current')
export const getUserById = (id) => request.get(`/users/${id}`)
export const createUser = (data) => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const deleteUser = (id) => request.delete(`/users/${id}`)
export const getUserWithStore = (id) => request.get(`/users/${id}/with-store`)
export const getUserWithRole = (id) => request.get(`/users/${id}/with-role`)
export const getUserFullDetails = (id) => request.get(`/users/${id}/full`)
export const getAllUsers = () => request.get('/users')
export const getUsersPage = (params) => request.get('/users/page', { params })

// ------ 角色 ------
export const getAllRoles = () => request.get('/roles')
export const getRoleById = (id) => request.get(`/roles/${id}`)
export const getRoleByCode = (code) => request.get(`/roles/code/${code}`)
export const createRole = (data) => request.post('/roles', data)
export const updateRole = (id, data) => request.put(`/roles/${id}`, data)
export const deleteRole = (id) => request.delete(`/roles/${id}`)
