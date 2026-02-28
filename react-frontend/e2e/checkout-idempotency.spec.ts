import { randomUUID } from 'node:crypto'
import { expect, test } from '@playwright/test'

const API_BASE_URL = process.env.E2E_API_BASE_URL ?? 'http://localhost:8080/api'
const USER_TOKEN_KEY = 'user_auth_token'
const PENDING_IDEMPOTENCY_KEY = 'checkout_pending_idempotency_key'

async function prepareBuyerAndPhone(request: any) {
  const resetRes = await request.post(`${API_BASE_URL}/e2e/reset`)
  expect(resetRes.ok()).toBeTruthy()

  const resetBody = await resetRes.json()
  const buyer = resetBody?.data?.buyer
  const phoneId = resetBody?.data?.phones?.[0]?.id

  expect(buyer?.email).toBeTruthy()
  expect(buyer?.password).toBeTruthy()
  expect(phoneId).toBeTruthy()

  const loginRes = await request.post(`${API_BASE_URL}/auth/login`, {
    data: {
      email: buyer.email,
      password: buyer.password,
    },
  })
  expect(loginRes.ok()).toBeTruthy()

  const loginBody = await loginRes.json()
  const token = loginBody?.data?.token
  expect(token).toBeTruthy()

  return {
    token,
    phoneId,
  }
}

async function addToCart(request: any, token: string, phoneId: string) {
  const response = await request.post(`${API_BASE_URL}/cart`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    data: {
      phoneId,
      quantity: 1,
    },
  })
  expect(response.ok()).toBeTruthy()
}

async function fetchOrderIds(request: any, token: string) {
  const response = await request.get(`${API_BASE_URL}/orders?page=1&pageSize=10`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })
  expect(response.ok()).toBeTruthy()

  const body = await response.json()
  const items = body?.data?.items ?? []
  return items.map((item: any) => item.id)
}

async function fillAddress(page: any) {
  await page.getByLabel('Street').fill('1 Main St')
  await page.getByLabel('City').fill('Sydney')
  await page.getByLabel('State').fill('NSW')
  await page.getByLabel('Zip').fill('2000')
  await page.getByLabel('Country').fill('AU')
}

test('checkout is idempotent for rapid clicks and refresh retry with same key', async ({ page, request }) => {
  const { token, phoneId } = await prepareBuyerAndPhone(request)
  await addToCart(request, token, phoneId)

  const idempotencyKey = randomUUID()
  let checkoutRequestCount = 0

  await page.route('**/api/orders/checkout', async (route) => {
    checkoutRequestCount += 1
    if (checkoutRequestCount === 1) {
      await new Promise((resolve) => setTimeout(resolve, 500))
    }
    await route.continue()
  })

  await page.addInitScript(
    ({ authToken, key }) => {
      window.localStorage.setItem('user_auth_token', authToken)
      window.sessionStorage.setItem('checkout_pending_idempotency_key', key)
    },
    { authToken: token, key: idempotencyKey },
  )

  await page.goto('/checkout')
  await expect(page.getByRole('heading', { name: 'Checkout' })).toBeVisible()

  await fillAddress(page)

  const submitButton = page.locator("button[type='submit']")
  await submitButton.dblclick()
  await expect(submitButton).toBeDisabled()
  await expect(page.getByText('Checkout successful')).toBeVisible()

  const orderIdLine = (await page.getByText(/^Order ID:/).first().textContent()) ?? ''
  const orderId = orderIdLine.replace('Order ID:', '').trim()
  expect(orderId).toBeTruthy()
  expect(checkoutRequestCount).toBe(1)

  const firstOrderIds = await fetchOrderIds(request, token)
  expect(firstOrderIds).toHaveLength(1)
  expect(firstOrderIds[0]).toBe(orderId)

  await addToCart(request, token, phoneId)

  await page.reload()
  await page.evaluate((key) => {
    window.sessionStorage.setItem('checkout_pending_idempotency_key', key)
  }, idempotencyKey)

  await fillAddress(page)
  await page.locator("button[type='submit']").click()
  await expect(page.getByText('Checkout successful')).toBeVisible()
  await expect(page.getByText(`Order ID: ${orderId}`)).toBeVisible()

  const secondOrderIds = await fetchOrderIds(request, token)
  expect(secondOrderIds).toHaveLength(1)
  expect(secondOrderIds[0]).toBe(orderId)
})
