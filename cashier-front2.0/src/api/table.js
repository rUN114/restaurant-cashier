import request from '@/utils/request'

// ------ 餐桌 ------
export const getTablesByStore = (storeId) => request.get(`/tables/store/${storeId}`)
export const getFreeTables = (storeId) => request.get(`/tables/free/${storeId}`)
export const updateTableStatus = (tableId, status) =>
  request.put(`/tables/status/${tableId}`, null, { params: { status } })
export const updateTable = (id, data) => request.put(`/tables/${id}`, data)
export const createTable = (data) => request.post('/tables', data)
export const deleteTable = (id) => request.delete(`/tables/${id}`)
export const getAllTables = () => request.get('/tables/all')
