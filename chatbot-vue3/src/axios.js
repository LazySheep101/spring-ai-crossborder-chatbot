import axios from "axios";

// 创建 Axios 实例
const instance = axios.create({
    baseURL: "/api", // 你的 API 基础 URL
    timeout: 7000, // 请求超时时间
})

instance.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      const url = err?.config?.url || ''
      if (String(url).includes('/admin/') && !String(url).includes('/admin/auth/login')) {
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_displayName')
        localStorage.removeItem('admin_role')
        const hash = window.location.hash || ''
        if (hash.startsWith('#/admin') && !hash.startsWith('#/admin/login')) {
          window.location.hash = '#/admin/login'
        }
      }
    }
    return Promise.reject(err)
  }
)

// 暴露出去
export default instance;
