import request from '@/utils/request'

export const getKitchenBoard = () => request.get('/kds/kitchen')
export const startCooking = (itemId) => request.post(`/kds/start/${itemId}`)
export const doneCooking = (itemId) => request.post(`/kds/done/${itemId}`)
export const getWaiterBoard = () => request.get('/kds/waiter')
export const bumpItem = (itemId) => request.post(`/kds/bump/${itemId}`)
export const serveItem = (itemId) => request.post(`/kds/serve/${itemId}`)
export const checkUnfinished = (orderId) => request.get(`/kds/unfinished/${orderId}`)
