import type { ReactNode } from 'react'
import { useLocation, Navigate, Outlet } from 'react-router-dom'
import type { AxiosError } from 'axios'
import { getApiErrorMessage } from '../../api'
import { ADMIN_TOKEN_KEY, USER_TOKEN_KEY, safeGetToken } from '../../auth/tokens'
import { useAdminAuth, useAuth } from '../../hooks'

type ProtectedRouteMode = 'user' | 'admin'

export type ProtectedRouteProps = {
  mode: ProtectedRouteMode
  loadingFallback?: ReactNode
}

function buildLoginRedirect(
  loginPath: string,
  currentPath: string,
  currentSearch: string,
  disableReturnUrl: boolean,
) {
  if (disableReturnUrl) return loginPath
  const returnUrl = `${currentPath}${currentSearch}`
  return `${loginPath}?returnUrl=${encodeURIComponent(returnUrl)}`
}

function getHttpStatus(error: unknown): number | undefined {
  const axiosError = error as AxiosError | null
  return axiosError?.response?.status
}

function DefaultLoading() {
  return <div>Loading...</div>
}

function UserProtectedRoute({ loadingFallback }: { loadingFallback?: ReactNode }) {
  const location = useLocation()
  const token = safeGetToken(USER_TOKEN_KEY)
  const hasToken = Boolean(token)

  const isOnLoginPage = location.pathname.startsWith('/login')
  const loginTo = buildLoginRedirect(
    '/login',
    location.pathname,
    location.search,
    isOnLoginPage,
  )

  const query = useAuth({ enabled: hasToken })

  if (!hasToken) {
    return <Navigate to={loginTo} replace />
  }

  if (query.isLoading || query.isFetching) {
    return <>{loadingFallback ?? <DefaultLoading />}</>
  }

  if (query.isError) {
    const status = getHttpStatus(query.error)
    if (status === 401 || status === 403) {
      return null
    }

    return (
      <div>
        <h1>系统错误</h1>
        <p>{getApiErrorMessage(query.error)}</p>
        <button type='button' onClick={() => query.refetch()}>
          重试
        </button>
      </div>
    )
  }

  return <Outlet />
}

function AdminProtectedRoute({ loadingFallback }: { loadingFallback?: ReactNode }) {
  const location = useLocation()
  const token = safeGetToken(ADMIN_TOKEN_KEY)
  const hasToken = Boolean(token)

  const isOnLoginPage = location.pathname.startsWith('/admin/login')
  const loginTo = buildLoginRedirect(
    '/admin/login',
    location.pathname,
    location.search,
    isOnLoginPage,
  )

  const query = useAdminAuth({ enabled: hasToken })

  if (!hasToken) {
    return <Navigate to={loginTo} replace />
  }

  if (query.isLoading || query.isFetching) {
    return <>{loadingFallback ?? <DefaultLoading />}</>
  }

  if (query.isError) {
    const status = getHttpStatus(query.error)
    if (status === 401 || status === 403) {
      return null
    }

    return (
      <div>
        <h1>系统错误</h1>
        <p>{getApiErrorMessage(query.error)}</p>
        <button type='button' onClick={() => query.refetch()}>
          重试
        </button>
      </div>
    )
  }

  return <Outlet />
}

export function ProtectedRoute({ mode, loadingFallback }: ProtectedRouteProps) {
  if (mode === 'admin') return <AdminProtectedRoute loadingFallback={loadingFallback} />
  return <UserProtectedRoute loadingFallback={loadingFallback} />
}
