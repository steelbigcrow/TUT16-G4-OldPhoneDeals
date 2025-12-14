import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { NotificationProvider } from '../../contexts/NotificationContext'
import { createTestQueryClient } from '../../test/testUtils'
import { ProfileLayoutPage } from './ProfileLayoutPage'

describe('ProfileLayoutPage', () => {
  it('renders navigation and the active outlet route', async () => {
    const router = createMemoryRouter(
      [
        {
          path: '/profile',
          element: <ProfileLayoutPage />,
          children: [{ index: true, element: <div>Profile home</div> }],
        },
      ],
      { initialEntries: ['/profile'] },
    )

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    expect(screen.getByText('Profile')).toBeInTheDocument()
    expect(await screen.findByText('Profile home')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Overview' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Listings' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Settings' })).toBeInTheDocument()
  })
})

