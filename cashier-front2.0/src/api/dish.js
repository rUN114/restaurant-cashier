import request from '@/utils/request'

// ------ 菜品分类 ------
export const getCategoriesByStore = (storeId) => request.get(`/categories/store/${storeId}`)
export const createCategory = (data) => request.post('/categories', data)
export const updateCategory = (id, data) => request.put(`/categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/categories/${id}`)
export const getAllCategories = () => request.get('/categories/all')

// ------ 菜品 ------
export const getDishesByStore = (storeId) => request.get(`/dishes/store/${storeId}`)
export const getAvailableDishes = (storeId) => request.get(`/dishes/available/${storeId}`)
export const createDish = (data) => request.post('/dishes', data)
export const updateDish = (id, data) => request.put(`/dishes/${id}`, data)
export const deleteDish = (id) => request.delete(`/dishes/${id}`)
export const getAllDishes = () => request.get('/dishes/all')
export const toggleDish = (id) => request.put(`/dishes/toggle/${id}`)

// 菜品图片上传
export const uploadDishImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/dishes/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
