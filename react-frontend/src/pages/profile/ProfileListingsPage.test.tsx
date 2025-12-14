import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ProfileListingsPage } from './ProfileListingsPage'
import { renderWithProviders } from '../../test/testUtils'
import { phonesApi, profileApi, uploadApi } from '../../api'

describe('ProfileListingsPage', () => {
  it('uploads an image and uses returned fileUrl when creating a listing', async () => {
    const user = userEvent.setup()

    vi.spyOn(profileApi, 'getProfile').mockResolvedValue({
      success: true,
      data: {
        id: 'u1',
        firstName: 'A',
        lastName: 'B',
        email: 'a@b.com',
        emailVerified: true,
        createdAt: null,
        updatedAt: null,
      },
    } as any)

    const sellerPhonesSpy = vi.spyOn(phonesApi, 'getPhonesBySeller').mockResolvedValue({
      success: true,
      data: [],
    } as any)

    const uploadSpy = vi.spyOn(uploadApi, 'uploadImage').mockResolvedValue({
      success: true,
      message: 'ok',
      data: {
        fileName: 'images/img.png',
        fileUrl: '/uploads/images/img.png',
        originalName: 'img.png',
        size: 3,
        contentType: 'image/png',
      },
    } as any)

    const createSpy = vi.spyOn(phonesApi, 'createPhone').mockResolvedValue({
      success: true,
      data: {
        id: 'p1',
        title: 'My phone',
      },
    } as any)

    renderWithProviders(<ProfileListingsPage />, { route: '/profile/listings' })

    await waitFor(() => expect(sellerPhonesSpy).toHaveBeenCalledWith('u1'))

    const fileInput = screen.getByLabelText('Upload image')
    const file = new File(['img'], 'img.png', { type: 'image/png' })
    await user.upload(fileInput, file)

    await waitFor(() => expect(uploadSpy).toHaveBeenCalledTimes(1))
    await waitFor(() =>
      expect((screen.getByLabelText('Image URL') as HTMLInputElement).value).toBe(
        '/uploads/images/img.png',
      ),
    )

    await user.type(screen.getByPlaceholderText('e.g. iPhone 6 16GB'), 'My phone')
    await user.click(screen.getByRole('button', { name: 'Create listing' }))

    await waitFor(() => expect(createSpy).toHaveBeenCalledTimes(1))
    expect(createSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'My phone',
        seller: 'u1',
        image: '/uploads/images/img.png',
      }),
    )
  })
})

