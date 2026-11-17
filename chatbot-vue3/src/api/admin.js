import axios from '@/axios.js'

function authHeaders(token) {
  const t = token || localStorage.getItem('admin_token')
  return t ? { Authorization: `Bearer ${t}` } : {}
}

export function adminLogin(data) {
  return axios.post('/admin/auth/login', data)
}

export function fetchDashboardSummary() {
  return axios.get('/admin/dashboard/summary', { headers: authHeaders() })
}

export function fetchProducts() {
  return axios.get('/admin/products', { headers: authHeaders() })
}

export function fetchCustomers() {
  return axios.get('/admin/customers', { headers: authHeaders() })
}

export function fetchOrders() {
  return axios.get('/admin/orders', { headers: authHeaders() })
}

export function fetchOrderDetail(orderNo) {
  return axios.get(`/admin/orders/${orderNo}`, { headers: authHeaders() })
}

export function fetchActions(status) {
  return axios.get('/admin/actions', { params: { status }, headers: authHeaders() })
}

export function reviewAction(id, data, token) {
  return axios.post(`/admin/actions/${id}/review`, data, {
    headers: authHeaders(token)
  })
}

export function fetchToolLogs(limit = 50) {
  return axios.get('/admin/tool-logs', { params: { limit }, headers: authHeaders() })
}

export function fetchTickets(status) {
  return axios.get('/admin/tickets', { params: { status }, headers: authHeaders() })
}

export function fetchTicketDetail(id) {
  return axios.get(`/admin/tickets/${id}`, { headers: authHeaders() })
}

export function replyTicket(id, data, token) {
  return axios.post(`/admin/tickets/${id}/reply`, data, {
    headers: authHeaders(token)
  })
}

export function takeoverTicket(id, data, token) {
  return axios.post(`/admin/tickets/${id}/takeover`, data, {
    headers: authHeaders(token)
  })
}

export function resolveTicket(id, data, token) {
  return axios.post(`/admin/tickets/${id}/resolve`, data, {
    headers: authHeaders(token)
  })
}

export function fetchTraces(limit = 50) {
  return axios.get('/admin/traces', { params: { limit }, headers: authHeaders() })
}

export function fetchTraceDetail(runId) {
  return axios.get(`/admin/traces/${runId}`, { headers: authHeaders() })
}

export function ragDebug(question, mode = 'current') {
  return axios.post('/admin/rag/debug', { question, mode }, { headers: authHeaders() })
}

export function fetchFileList(data) {
  return axios.post('/customer-service/file/list', data)
}

export function deleteMarkdownFile(id) {
  return axios.post('/customer-service/file/delete', { id })
}

export function fetchConversations(limit = 80) {
  return axios.get('/admin/conversations', { params: { limit }, headers: authHeaders() })
}

export function fetchConversation(chatId) {
  return axios.get(`/admin/conversations/${chatId}`, { headers: authHeaders() })
}

export function fetchAudit(limit = 80) {
  return axios.get('/admin/audit', { params: { limit }, headers: authHeaders() })
}

export function fetchUsers() {
  return axios.get('/admin/users', { headers: authHeaders() })
}

export function fetchEvalSummary() {
  return axios.get('/admin/evals/summary', { headers: authHeaders() })
}

export function fetchAgentWorkflow() {
  return axios.get('/admin/agent-workflow', { headers: authHeaders() })
}

export function fetchMultilingual(limit = 50) {
  return axios.get('/admin/multilingual', { params: { limit }, headers: authHeaders() })
}
