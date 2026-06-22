import request from '@/utils/request'

// ------ 订单 ------
export const getOrdersByStore = (storeId) => request.get(`/orders/store/${storeId}`)
export const getInProgressOrders = (storeId) => request.get(`/orders/progress/${storeId}`)
export const createOrder = (data) => request.post('/orders', data)
export const settleOrder = (orderId, cashierId, paymentMethod, discountAmount, finalAmount, memberId, useMemberDiscount, redeemPoints, requestId) =>
  request.put(`/orders/settle/${orderId}`, null, {
    params: { cashierId, paymentMethod, discountAmount, finalAmount, memberId, useMemberDiscount, redeemPoints, requestId },
  })
export const cancelOrder = (orderId) => request.put(`/orders/cancel/${orderId}`)
export const getOrderById = (id) => request.get(`/orders/${id}`)
export const getAllOrders = () => request.get('/orders/all')
export const getOrdersPage = (params) => request.get('/orders/page', { params })

// ------ 订单明细 ------
export const getOrderItemsByOrderId = (orderId) => request.get(`/order-items/order/${orderId}`)
export const addOrderItem = (data) => request.post('/order-items', data)
export const updateOrderItem = (id, data) => request.put(`/order-items/${id}`, data)
export const deleteOrderItem = (id) => request.delete(`/order-items/${id}`)

// ------ 营业额统计 ------
export const getTodayTurnover = (storeId) =>
  request.get('/statistics/today-turnover', { params: { storeId } })
export const getMonthTurnover = (storeId) =>
  request.get('/statistics/month-turnover', { params: { storeId } })
