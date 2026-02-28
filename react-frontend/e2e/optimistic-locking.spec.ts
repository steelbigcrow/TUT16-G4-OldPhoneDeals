import { expect, test } from '@playwright/test'

const API_BASE_URL = process.env.E2E_API_BASE_URL ?? 'http://localhost:8080/api'

async function resetAndLoginSeller(request: any) {
  const resetRes = await request.post(`${API_BASE_URL}/e2e/reset`)
  expect(resetRes.ok()).toBeTruthy()

  const resetBody = await resetRes.json()
  const seller = resetBody?.data?.seller
  const phoneId = resetBody?.data?.phones?.[0]?.id

  expect(seller?.email).toBeTruthy()
  expect(seller?.password).toBeTruthy()
  expect(phoneId).toBeTruthy()

  const loginRes = await request.post(`${API_BASE_URL}/auth/login`, {
    data: {
      email: seller.email,
      password: seller.password,
    },
  })
  expect(loginRes.ok()).toBeTruthy()

  const loginBody = await loginRes.json()
  const token = loginBody?.data?.token
  expect(token).toBeTruthy()

  return { token, phoneId }
}

async function fetchPhoneVersion(request: any, phoneId: string) {
  const res = await request.get(`${API_BASE_URL}/phones/${phoneId}`)
  expect(res.ok()).toBeTruthy()

  const body = await res.json()
  const version = body?.data?.version
  expect(version).not.toBeNull()
  expect(version).not.toBeUndefined()

  return version as number
}

test('phone update with stale version returns 409 conflict', async ({ request }) => {
  const { token, phoneId } = await resetAndLoginSeller(request)

  const originalVersion = await fetchPhoneVersion(request, phoneId)

  const okUpdate = await request.put(`${API_BASE_URL}/phones/${phoneId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    data: {
      price: 1099.0,
      version: originalVersion,
    },
  })
  expect(okUpdate.ok()).toBeTruthy()

  const conflictUpdate = await request.put(`${API_BASE_URL}/phones/${phoneId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    data: {
      price: 1199.0,
      version: originalVersion,
    },
  })

  expect(conflictUpdate.status()).toBe(409)
  const conflictBody = await conflictUpdate.json()
  expect(conflictBody?.success).toBe(false)
  expect(String(conflictBody?.message ?? '')).toContain('Version conflict')
})

