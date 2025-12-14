import { describe, expect, it, vi } from 'vitest'
import { z } from 'zod'
import { setZodFormErrors } from './zodFormErrors'

describe('setZodFormErrors()', () => {
  it('maps Zod issues to react-hook-form setError calls', () => {
    const schema = z.object({
      email: z.string().email('Invalid email'),
      password: z.string().min(6, 'Too short'),
    })

    const parsed = schema.safeParse({ email: 'not-an-email', password: '123' })
    expect(parsed.success).toBe(false)

    const setError = vi.fn()
    setZodFormErrors((parsed as any).error, setError as any)

    expect(setError).toHaveBeenCalledWith('email', { type: 'validate', message: 'Invalid email' })
    expect(setError).toHaveBeenCalledWith('password', { type: 'validate', message: 'Too short' })
  })
})

