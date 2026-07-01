import request from '@/utils/request'

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

// ------ 原料库存 ------
export const getInventoryByStore = (storeId) => request.get(`/inventory/store/${storeId}`)
export const adjustInventory = (data) => request.post('/inventory/adjust', data)
const cleanParams = (obj) => { for (const k in obj) if (obj[k] == null) delete obj[k]; return obj }
export const getLowStock = (storeId) => request.get('/inventory/low-stock', { params: cleanParams({ storeId }) })
export const getStockRecords = (storeId) => request.get('/inventory/records', { params: cleanParams({ storeId }) })
export const getPurchaseHistory = (storeId, materialId) => request.get('/inventory/purchase-history', { params: cleanParams({ storeId, materialId }) })
export const backfillCostPrice = () => request.post('/inventory/backfill-cost-price')
