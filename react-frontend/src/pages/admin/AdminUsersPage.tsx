import { useMemo, useState } from 'react'
import { z } from 'zod'
import { getApiErrorMessage } from '../../api'
import { useAdminUsers, useToggleAdminUserDisabled } from '../../hooks'
import { useNotifications } from '../../contexts/NotificationContext'

const filterSchema = z.object({
  search: z.string().optional(),
  isDisabled: z.enum(['all', 'true', 'false']).default('all'),
})

export function AdminUsersPage() {
  const notifications = useNotifications()

  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize] = useState(10)

  const [searchDraft, setSearchDraft] = useState('')
  const [disabledFilter, setDisabledFilter] = useState<'all' | 'true' | 'false'>('all')

  const parsedFilters = useMemo(() => {
    const parsed = filterSchema.safeParse({ search: searchDraft, isDisabled: disabledFilter })
    if (!parsed.success) return { search: undefined, isDisabled: undefined as boolean | undefined }
    return {
      search: parsed.data.search?.trim() ? parsed.data.search.trim() : undefined,
      isDisabled:
        parsed.data.isDisabled === 'all' ? undefined : parsed.data.isDisabled === 'true',
    }
  }, [searchDraft, disabledFilter])

  const query = useAdminUsers({
    pageIndex,
    pageSize,
    search: parsedFilters.search,
    isDisabled: parsedFilters.isDisabled,
  })

  const toggleDisabled = useToggleAdminUserDisabled()

  const page = query.data?.success ? query.data.data : undefined
  const users = page?.content ?? []

  return (
    <div className='space-y-4'>
      <div>
        <h2 className='text-base font-semibold'>Users</h2>
        <p className='mt-1 text-sm text-slate-600'>Search and manage user accounts.</p>
      </div>

      <div className='grid grid-cols-1 gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:grid-cols-6'>
        <div className='sm:col-span-4'>
          <label className='text-xs font-medium text-slate-700'>Search</label>
          <input
            value={searchDraft}
            onChange={(e) => setSearchDraft(e.target.value)}
            placeholder='Name or email'
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          />
        </div>
        <div className='sm:col-span-2'>
          <label className='text-xs font-medium text-slate-700'>Disabled</label>
          <select
            value={disabledFilter}
            onChange={(e) => setDisabledFilter(e.target.value as any)}
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          >
            <option value='all'>All</option>
            <option value='true'>Disabled</option>
            <option value='false'>Active</option>
          </select>
        </div>
        <div className='sm:col-span-6 flex items-center justify-between'>
          <button
            type='button'
            onClick={() => setPageIndex(0)}
            className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50'
          >
            Apply
          </button>
          <div className='text-sm text-slate-600'>
            Page <span className='font-medium text-slate-900'>{page?.currentPage ?? 1}</span> /{' '}
            <span className='font-medium text-slate-900'>{page?.totalPages ?? 1}</span>
          </div>
        </div>
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
                <th className='px-4 py-3'>Name</th>
                <th className='px-4 py-3'>Email</th>
                <th className='px-4 py-3'>Role</th>
                <th className='px-4 py-3'>Status</th>
                <th className='px-4 py-3'></th>
              </tr>
            </thead>
            <tbody className='divide-y divide-slate-200'>
              {users.map((u) => (
                <tr key={u.id}>
                  <td className='px-4 py-3 font-medium'>
                    {u.firstName} {u.lastName}
                  </td>
                  <td className='px-4 py-3'>{u.email}</td>
                  <td className='px-4 py-3'>{u.role}</td>
                  <td className='px-4 py-3'>
                    {u.isDisabled ? (
                      <span className='rounded-md bg-rose-50 px-2 py-1 text-xs font-medium text-rose-700'>
                        Disabled
                      </span>
                    ) : (
                      <span className='rounded-md bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700'>
                        Active
                      </span>
                    )}
                  </td>
                  <td className='px-4 py-3 text-right'>
                    <button
                      type='button'
                      disabled={toggleDisabled.isPending}
                      onClick={async () => {
                        try {
                          const res = await toggleDisabled.mutateAsync(u.id)
                          if (res.success) notifications.success('User status updated')
                          else notifications.error(res.message ?? 'Failed to update user')
                        } catch (err) {
                          notifications.error(getApiErrorMessage(err))
                        }
                      }}
                      className='rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium hover:bg-slate-50 disabled:opacity-50'
                    >
                      Toggle disabled
                    </button>
                  </td>
                </tr>
              ))}
              {users.length === 0 ? (
                <tr>
                  <td className='px-4 py-8 text-center text-sm text-slate-600' colSpan={5}>
                    No users found.
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

