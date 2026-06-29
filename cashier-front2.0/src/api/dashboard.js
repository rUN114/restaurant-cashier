import request from '@/utils/request'

export const getDashboard = () => request.get('/dashboard/today')
export const getBigScreen = () => request.get('/dashboard/bigscreen')
