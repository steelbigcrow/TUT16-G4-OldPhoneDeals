import { useState } from 'react'
import { getApiErrorMessage } from '../../api'
import { useAdminLogs } from '../../hooks'

function formatDate(value: string | null | undefined) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString()
}

export function AdminLogsPage() {
  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize] = useState(10)

  const query = useAdminLogs({ pageIndex, pageSize })
  const page = query.data?.success ? query.data.data : undefined
  const logs = page?.content ?? []

  return (
    <div className='space-y-4'>
      <div>
        <h2 className='text-base font-semibold'>Logs</h2>
        <p className='mt-1 text-sm text-slate-600'>Audit trail for admin actions.</p>
      </div>

      {query.isLoading ? <div>Loading…</div> : null}
      {query.isError ? (
        <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
          {getApiErrorMessage(query.error)}
        </div>
      ) : null}

      {page ? (
        <div className='overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm'>
          <table className='w-full text-left text-sm'>
            <thead className='bg-slate-50 text-xs font-semibold text-slate-700'>
              <tr>
                <th className='px-4 py-3'>When</th>
                <th className='px-4 py-3'>Admin</th>
                <th className='px-4 py-3'>Action</th>
                <th className='px-4 py-3'>Target</th>
                <th className='px-4 py-3'>Details</th>
              </tr>
            </thead>
            <tbody className='divide-y divide-slate-200'>
              {logs.map((l) => (
                <tr key={l.id}>
                  <td className='px-4 py-3'>{formatDate(l.createdAt)}</td>
                  <td className='px-4 py-3'>{l.adminName}</td>
                  <td className='px-4 py-3'>
                    <span className='font-mono text-xs'>{l.action}</span>
                  </td>
                  <td className='px-4 py-3'>
                    <div className='text-xs font-mono'>{l.targetType}</div>
                    <div className='text-xs text-slate-600'>{l.targetId}</div>
                  </td>
                  <td className='px-4 py-3'>
                    <div className='max-w-[32rem] truncate' title={l.details ?? ''}>
                      {l.details ?? '—'}
                    </div>
                  </td>
                </tr>
              ))}
              {logs.length === 0 ? (
                <tr>
                  <td className='px-4 py-8 text-center text-sm text-slate-600' colSpan={5}>
                    No logs found.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      ) : null}

      <div className='flex items-center justify-between'>
        <button
          type='button'
          disabled={pageIndex <= 0}
          onClick={() => setPageIndex((p) => Math.max(0, p - 1))}
          className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
        >
          Previous
        </button>
        <button
          type='button'
          disabled={pageIndex + 1 >= (page?.totalPages ?? 1)}
          onClick={() => setPageIndex((p) => p + 1)}
          className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
        >
          Next
        </button>
      </div>
    </div>
  )
}

