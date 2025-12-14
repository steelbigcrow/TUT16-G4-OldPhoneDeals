import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ProfileSettingsPage } from './ProfileSettingsPage'
import { renderWithProviders } from '../../test/testUtils'
import { profileApi } from '../../api'

describe('ProfileSettingsPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('prefills profile form and submits updates', async () => {
    const user = userEvent.setup()

    const getProfileSpy = vi.spyOn(profileApi, 'getProfile').mockResolvedValue({
      success: true,
      data: {
        id: 'u1',
        firstName: 'Alice',
        lastName: 'Smith',
        email: 'alice@example.com',
        emailVerified: true,
        createdAt: null,
        updatedAt: null,
      },
    } as any)

    const updateProfileSpy = vi.spyOn(profileApi, 'updateProfile').mockResolvedValue({
      success: true,
      data: {
        id: 'u1',
        firstName: 'Alicia',
        lastName: 'Smith',
        email: 'alice@example.com',
        emailVerified: true,
        createdAt: null,
        updatedAt: null,
      },
    } as any)

    renderWithProviders(<ProfileSettingsPage />, { route: '/profile/settings' })

    await waitFor(() => expect(getProfileSpy).toHaveBeenCalled())

    await waitFor(() =>
      expect((screen.getByLabelText('First name') as HTMLInputElement).value).toBe('Alice'),
    )
    expect((screen.getByLabelText('Last name') as HTMLInputElement).value).toBe('Smith')
    expect((screen.getByLabelText('Email') as HTMLInputElement).value).toBe('alice@example.com')

    await user.clear(screen.getByLabelText('First name'))
    await user.type(screen.getByLabelText('First name'), 'Alicia')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() =>
      expect(updateProfileSpy).toHaveBeenCalledWith({
        firstName: 'Alicia',
        lastName: 'Smith',
        email: 'alice@example.com',
        currentPassword: '',
      }),
    )

    expect(await screen.findByText('Profile updated')).toBeInTheDocument()
  })
})

