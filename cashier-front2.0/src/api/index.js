// API 模块统一导出（re-export barrel）
// 按领域拆分到独立文件，此处集中重导出，保持原 import { xxx } from '@/api' 路径不变

export {
  // 菜品分类
  getCategoriesByStore,
  createCategory,
  updateCategory,
  deleteCategory,
  getAllCategories,
  // 菜品
  getDishesByStore,
  getAvailableDishes,
  createDish,
  updateDish,
  deleteDish,
  getAllDishes,
  uploadDishImage,
} from './dish'

export {
  // 订单
  getOrdersByStore,
  getInProgressOrders,
  createOrder,
  settleOrder,
  cancelOrder,
  reverseSettleOrder,
  getOrderById,
  getAllOrders,
  getOrdersPage,
  // 订单明细
  getOrderItemsByOrderId,
  addOrderItem,
  updateOrderItem,
  deleteOrderItem,
  // 营业额统计
  getTodayTurnover,
  getMonthTurnover,
} from './order'

export {
  getTablesByStore,
  getFreeTables,
  updateTableStatus,
  updateTable,
  createTable,
  deleteTable,
  getAllTables,
} from './table'

export {
  getValidStores,
  getStoreById,
  createStore,
  updateStore,
  deleteStore,
  getAllStores,
} from './store'

export {
  login,
  getCurrentUser,
  getUserById,
  createUser,
  updateUser,
  deleteUser,
  getUserWithStore,
  getUserWithRole,
  getUserFullDetails,
  getAllUsers,
  getUsersPage,
  getAllRoles,
  getRoleById,
  getRoleByCode,
  createRole,
  updateRole,
  deleteRole,
} from './user'

export {
  getAllLoginLogs,
  getLoginLogsPage,
  createLoginLog,
  getAllOperationLogs,
  getOperationLogsPage,
  createOperationLog,
  getOrderItemLogsByOrderId,
  createOrderItemLog,
  getAllOrderItemLogs,
  getChangelogs,
  getLatestChangelog,
  createChangelog,
  updateChangelog,
  deleteChangelog,
} from './log'

export {
  getMaterialsByStore,
  getAllMaterials,
  createMaterial,
  updateMaterial,
  deleteMaterial,
  getPurchasesByStore,
  getPurchasesByStorePage,
  getAllPurchases,
  createPurchase,
  getInventoryByStore,
  adjustInventory,
  getLowStock,
  getStockRecords,
} from './inventory'

export {
  getRecipesByDish,
  saveRecipe,
  deleteRecipe,
  getConsumptionsByOrder,
  getAllConsumptions,
  getCostsByStore,
  createCost,
  updateCost,
  deleteCost,
  getConfigValue,
  getAllConfigs,
  updateConfig,
} from './cost'

export { getDashboard } from './dashboard'
