import request from '@/utils/request'

// ------ 门店 ------
export const getValidStores = () => request.get('/stores/valid')
export const getStoreById = (id) => request.get(`/stores/${id}`)
export const createStore = (data) => request.post('/stores', data)
export const updateStore = (id, data) => request.put(`/stores/${id}`, data)
export const deleteStore = (id) => request.delete(`/stores/${id}`)
export const getAllStores = () => request.get('/stores/all')
