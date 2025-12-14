import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { cn } from '../utils/cn'

type ToastType = 'success' | 'error' | 'info'

type Toast = {
  id: string
  type: ToastType
  title?: string
  message: string
}

type NotifyInput = Omit<Toast, 'id'>

type NotificationContextValue = {
  toasts: Toast[]
  notify: (toast: NotifyInput) => void
  success: (message: string, title?: string) => void
  error: (message: string, title?: string) => void
  info: (message: string, title?: string) => void
  dismiss: (id: string) => void
  clear: () => void
}

const NotificationContext = createContext<NotificationContextValue | null>(null)

function makeId() {
  return `${Date.now()}_${Math.random().toString(16).slice(2)}`
}

function typeStyles(type: ToastType) {
  switch (type) {
    case 'success':
      return 'border-emerald-200 bg-emerald-50 text-emerald-950'
    case 'error':
      return 'border-rose-200 bg-rose-50 text-rose-950'
    case 'info':
      return 'border-sky-200 bg-sky-50 text-sky-950'
  }
}

export function NotificationProvider(props: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const dismiss = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const clear = useCallback(() => {
    setToasts([])
  }, [])

  const notify = useCallback((toast: NotifyInput) => {
    const id = makeId()
    setToasts((prev) => [{ ...toast, id }, ...prev].slice(0, 5))

    window.setTimeout(() => {
      dismiss(id)
    }, 4500)
  }, [dismiss])

  const value = useMemo<NotificationContextValue>(
    () => ({
      toasts,
      notify,
      success: (message, title) => notify({ type: 'success', message, title }),
      error: (message, title) => notify({ type: 'error', message, title }),
      info: (message, title) => notify({ type: 'info', message, title }),
      dismiss,
      clear,
    }),
    [toasts, notify, dismiss, clear],
  )

  return (
    <NotificationContext.Provider value={value}>
      {props.children}

      <div className='pointer-events-none fixed right-4 top-4 z-50 flex w-[min(420px,calc(100vw-2rem))] flex-col gap-2'>
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={cn(
              'pointer-events-auto rounded-lg border p-3 shadow-sm',
              typeStyles(toast.type),
            )}
          >
            <div className='flex items-start gap-3'>
              <div className='min-w-0 flex-1'>
                {toast.title ? (
                  <div className='text-sm font-semibold'>{toast.title}</div>
                ) : null}
                <div className='text-sm opacity-90'>{toast.message}</div>
              </div>
              <button
                type='button'
                onClick={() => dismiss(toast.id)}
                className='rounded-md px-2 py-1 text-xs font-medium opacity-70 hover:opacity-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-slate-400'
              >
                ×
              </button>
            </div>
          </div>
        ))}
      </div>
    </NotificationContext.Provider>
  )
}

export function useNotifications() {
  const ctx = useContext(NotificationContext)
  if (!ctx) {
    throw new Error('useNotifications must be used within NotificationProvider')
  }
  return ctx
}

