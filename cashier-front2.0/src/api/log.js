import request from '@/utils/request'

// ------ 登录日志 ------
export const getAllLoginLogs = () => request.get('/login-logs')
export const getLoginLogsPage = (params) => request.get('/login-logs/page', { params })
export const createLoginLog = (data) => request.post('/login-logs', data)

// ------ 操作日志 ------
export const getAllOperationLogs = () => request.get('/operation-logs')
export const getOperationLogsPage = (params) => request.get('/operation-logs/page', { params })
export const createOperationLog = (data) => request.post('/operation-logs', data)

// ------ 菜品流水 ------
export const getOrderItemLogsByOrderId = (orderId) =>
  request.get(`/order-item-logs/order/${orderId}`)
export const createOrderItemLog = (data) => request.post('/order-item-logs', data)
export const getAllOrderItemLogs = (params) => request.get('/order-item-logs', { params })

// ------ 更新日志 ------
export const getChangelogs = () => request.get('/changelogs')
export const getLatestChangelog = () => request.get('/changelogs/latest')
export const createChangelog = (data) => request.post('/changelogs', data)
export const updateChangelog = (id, data) => request.put(`/changelogs/${id}`, data)
export const deleteChangelog = (id) => request.delete(`/changelogs/${id}`)
