import request from '@/utils/request'

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

// ------ 系统配置 ------
export const getConfigValue = (key) => request.get(`/sys-configs/key/${key}`)
export const getAllConfigs = () => request.get('/sys-configs')
export const updateConfig = (data) => request.put('/sys-configs', data)
