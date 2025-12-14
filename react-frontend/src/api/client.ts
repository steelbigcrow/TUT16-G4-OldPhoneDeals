import axios, { type AxiosError, type AxiosInstance } from 'axios'
import { normalizePath } from './normalizePath'
import { clearTokenForPath, getTokenForPath } from '../auth/tokens'

type ApiErrorData = {
  message?: string
}

type RequestConfigWithMeta = {
  __normalizedPath?: string
}

function buildLoginRedirectUrl(normalizedPath: string): string {
  const isAdminRequest = normalizedPath.startsWith('/admin')
  const loginPath = isAdminRequest ? '/admin/login' : '/login'

  if (typeof window === 'undefined') return loginPath

  const currentPath = window.location.pathname
  const currentSearch = window.location.search
  const currentFullPath = `${currentPath}${currentSearch}`

  const currentIsUserLogin = currentPath.startsWith('/login')
  const currentIsAdminLogin = currentPath.startsWith('/admin/login')
  const currentIsLogin = currentIsUserLogin || currentIsAdminLogin

  if (currentIsLogin) return loginPath

  return `${loginPath}?returnUrl=${encodeURIComponent(currentFullPath)}`
}

export const navigation = {
  assign: (url: string) => window.location.assign(url),
}

export function getApiErrorMessage(error: unknown): string {
  const axiosError = error as AxiosError<ApiErrorData> | null
  const apiMessage = axiosError?.response?.data?.message
  if (apiMessage) return apiMessage
  if (axiosError?.message) return axiosError.message
  return '网络错误'
}

export const apiClient: AxiosInstance = axios.create({
  baseURL: '/api',
})

apiClient.interceptors.request.use((config) => {
  const normalizedPath = normalizePath(config.url)
  ;(config as RequestConfigWithMeta).__normalizedPath = normalizedPath

  const token = getTokenForPath(normalizedPath)
  if (token) {
    config.headers = config.headers ?? {}
    ;(config.headers as Record<string, string>).Authorization = `Bearer ${token}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    const axiosError = error as AxiosError<ApiErrorData> | null
    const status = axiosError?.response?.status
    const config = axiosError?.config as (RequestConfigWithMeta & { url?: string }) | undefined

    const normalizedPath =
      config?.__normalizedPath ?? normalizePath(config?.url ?? undefined)

    if ((status === 401 || status === 403) && typeof window !== 'undefined') {
      clearTokenForPath(normalizedPath)
      const redirectUrl = buildLoginRedirectUrl(normalizedPath)
      navigation.assign(redirectUrl)
    }

    return Promise.reject(error)
  },
)
