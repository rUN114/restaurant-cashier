import request from '@/utils/request'

// ------ 菜品分类 ------
export const getCategoriesByStore = (storeId) => request.get(`/categories/store/${storeId}`)
export const createCategory = (data) => request.post('/categories', data)
export const updateCategory = (id, data) => request.put(`/categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/categories/${id}`)

// ------ 菜品 ------
export const getDishesByStore = (storeId) => request.get(`/dishes/store/${storeId}`)
export const getAvailableDishes = (storeId) => request.get(`/dishes/available/${storeId}`)
export const createDish = (data) => request.post('/dishes', data)
export const updateDish = (id, data) => request.put(`/dishes/${id}`, data)
export const deleteDish = (id) => request.delete(`/dishes/${id}`)

// ------ 订单 ------
export const getOrdersByStore = (storeId) => request.get(`/orders/store/${storeId}`)
export const getInProgressOrders = (storeId) => request.get(`/orders/progress/${storeId}`)
export const createOrder = (data) => request.post('/orders', data)
export const settleOrder = (orderId, cashierId, paymentMethod, discountAmount, finalAmount, memberId, useMemberDiscount, redeemPoints) =>
  request.put(`/orders/settle/${orderId}`, null, {
    params: { cashierId, paymentMethod, discountAmount, finalAmount, memberId, useMemberDiscount, redeemPoints },
  })
export const cancelOrder = (orderId) => request.put(`/orders/cancel/${orderId}`)
export const getOrderById = (id) => request.get(`/orders/${id}`)
// 营业额统计已改用新接口
export const getTodayTurnover = (storeId) =>
  request.get('/statistics/today-turnover', { params: { storeId } })
export const getMonthTurnover = (storeId) =>
  request.get('/statistics/month-turnover', { params: { storeId } })

// ------ 订单明细 ------
export const getOrderItemsByOrderId = (orderId) => request.get(`/order-items/order/${orderId}`)
export const addOrderItem = (data) => request.post('/order-items', data)
export const updateOrderItem = (id, data) => request.put(`/order-items/${id}`, data)
export const deleteOrderItem = (id) => request.delete(`/order-items/${id}`)

// ------ 餐桌 ------
export const getTablesByStore = (storeId) => request.get(`/tables/store/${storeId}`)
export const getFreeTables = (storeId) => request.get(`/tables/free/${storeId}`)
export const updateTableStatus = (tableId, status) =>
  request.put(`/tables/status/${tableId}`, null, { params: { status } })
export const updateTable = (id, data) => request.put(`/tables/${id}`, data)
export const createTable = (data) => request.post('/tables', data)
export const deleteTable = (id) => request.delete(`/tables/${id}`)

// ------ 门店 ------
export const getValidStores = () => request.get('/stores/valid')
export const getStoreById = (id) => request.get(`/stores/${id}`)
export const createStore = (data) => request.post('/stores', data)
export const updateStore = (id, data) => request.put(`/stores/${id}`, data)
export const deleteStore = (id) => request.delete(`/stores/${id}`)
export const getAllStores = () => request.get('/stores/all')

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

// ------ 日志 ------
export const getAllLoginLogs = () => request.get('/login-logs')
export const getLoginLogsPage = (params) => request.get('/login-logs/page', { params })
export const createLoginLog = (data) => request.post('/login-logs', data)
export const getAllOperationLogs = () => request.get('/operation-logs')
export const getOperationLogsPage = (params) => request.get('/operation-logs/page', { params })
export const createOperationLog = (data) => request.post('/operation-logs', data)
export const getOrderItemLogsByOrderId = (orderId) =>
  request.get(`/order-item-logs/order/${orderId}`)
export const createOrderItemLog = (data) => request.post('/order-item-logs', data)

// 系统配置
export const getConfigValue = (key) => request.get(`/sys-configs/key/${key}`)
export const getAllConfigs = () => request.get('/sys-configs')
export const updateConfig = (data) => request.put('/sys-configs', data)

// 菜品图片上传
export const uploadDishImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/dishes/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 获取所有菜品（管理员）
export const getAllDishes = () => request.get('/dishes/all')

// 获取所有分类
export const getAllCategories = () => request.get('/categories/all')

export const getAllTables = () => request.get('/tables/all')
// 获取所有订单（管理员）
export const getAllOrders = () => request.get('/orders/all')
// 分页查询订单
export const getOrdersPage = (params) => request.get('/orders/page', { params })

// 获取所有菜品操作流水（可传 params: { storeId }）
export const getAllOrderItemLogs = (params) => request.get('/order-item-logs', { params })

// ------ 菜品配方 ------
export const getRecipesByDish = (dishId) => request.get(`/recipes/dish/${dishId}`)
export const saveRecipe = (data) => request.post('/recipes', data)
export const deleteRecipe = (id) => request.delete(`/recipes/${id}`)

// ------ 原材料消耗 ------
export const getConsumptionsByOrder = (orderId) => request.get(`/consumptions/order/${orderId}`)
export const getAllConsumptions = (params) => request.get('/consumptions', { params })

// ------ 固定成本管理 ------
export const getCostsByStore = (storeId) => request.get(`/fixed_costs/store/${storeId}`)
export const createCost = (data) => request.post('/fixed_costs', data)
export const updateCost = (id, data) => request.put(`/fixed_costs/${id}`, data)
export const deleteCost = (id) => request.delete(`/fixed_costs/${id}`)

// ------ 原料库存 ------
export const getInventoryByStore = (storeId) => request.get(`/inventory/store/${storeId}`)
export const adjustInventory = (data) => request.post('/inventory/adjust', data)
export const getLowStock = (storeId) => request.get('/inventory/low-stock', { params: { storeId } })
export const getStockRecords = (storeId) => request.get('/inventory/records', { params: { storeId } })

// ------ 更新日志 ------
export const getChangelogs = () => request.get('/changelogs')
export const getLatestChangelog = () => request.get('/changelogs/latest')
export const createChangelog = (data) => request.post('/changelogs', data)
export const updateChangelog = (id, data) => request.put(`/changelogs/${id}`, data)
export const deleteChangelog = (id) => request.delete(`/changelogs/${id}`)

// ------ 原材料档案 ------
export const getMaterialsByStore = (storeId) => request.get(`/materials/store/${storeId}`)
export const getAllMaterials = () => request.get('/materials/all')
export const createMaterial = (data) => request.post('/materials', data)
export const updateMaterial = (id, data) => request.put(`/materials/${id}`, data)
export const deleteMaterial = (id) => request.delete(`/materials/${id}`)

// ------ 采购入库管理 ------
export const getPurchasesByStore = (storeId) => request.get(`/purchases/store/${storeId}`)
export const getPurchasesByStorePage = (storeId, params) => request.get(`/purchases/page/${storeId}`, { params })
export const getAllPurchases = () => request.get('/purchases/all')
export const createPurchase = (data) => request.post('/purchases', data)
