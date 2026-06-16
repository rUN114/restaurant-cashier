import request from '@/utils/request'

// ==================== 会员 CRUD ====================
export const getMembersPage = (params) => request.get('/members/page', { params })
export const getMemberById = (id) => request.get(`/members/${id}`)
export const searchMemberByPhone = (phone) => request.get('/members/search', { params: { phone } })
export const registerMember = (data) => request.post('/members/register', data)
export const createMember = (data) => request.post('/members', data)
export const updateMember = (id, data) => request.put(`/members/${id}`, data)
export const toggleMemberStatus = (id, status) => request.put(`/members/${id}/status`, null, { params: { status } })

// ==================== 积分 ====================
export const adjustPoints = (data) => request.post('/members/points/adjust', data)
export const getPointLogs = (memberId, params) => request.get(`/members/${memberId}/point-logs`, { params })

// ==================== 会员等级配置 ====================
export const getMemberLevels = () => request.get('/member-levels')
export const createMemberLevel = (data) => request.post('/member-levels', data)
export const updateMemberLevel = (id, data) => request.put(`/member-levels/${id}`, data)
export const deleteMemberLevel = (id) => request.delete(`/member-levels/${id}`)

// ==================== 收银台结算 ====================
export const calcMemberCheckout = (params) => request.post('/member-checkout/calculate', null, { params })
