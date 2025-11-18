import { test, expect } from '@playwright/test';

type SeedUser = {
  email: string;
  password: string;
};

type SeedPhone = {
  id: string;
  title: string;
};

type SeedResponse = {
  buyer: SeedUser;
  phones: SeedPhone[];
};

const API_BASE = process.env.PLAYWRIGHT_API_BASE ?? 'http://127.0.0.1:8080/api';

let seedData: SeedResponse;

test.beforeEach(async ({ request }) => {
  const response = await request.post(`${API_BASE}/e2e/reset`);
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body?.success).toBeTruthy();
  seedData = body.data as SeedResponse;
});

test('buyer can browse catalog, wishlist a phone, and checkout', async ({
  page
}) => {
  const phone = seedData.phones[0];
  await page.goto('/');
  await expect(page.getByRole('heading', { name: /Old Phone Deals/i })).toBeVisible();
  await expect(page.locator('article').filter({ hasText: phone.title }).first()).toBeVisible();

  // 登录
  await page.getByRole('link', { name: 'Login' }).click();
  await page.getByLabel('Email').fill(seedData.buyer.email);
  await page.getByLabel('Password').fill(seedData.buyer.password);
  await page.getByRole('button', { name: 'Login as User' }).click();
  await expect(page.getByText('Hello, E2E')).toBeVisible();

  // 搜索指定商品
  await page.getByLabel('Search phones').fill(phone.title);
  await page.getByRole('button', { name: 'Search' }).click();
  await expect(page).toHaveURL(/search/);
  const searchCard = page.locator('article').filter({ hasText: phone.title }).first();
  await expect(searchCard).toBeVisible();
  await searchCard.getByRole('link', { name: 'View Details' }).click();

  // 商品详情页
  await expect(page).toHaveURL(new RegExp(`/phone/${phone.id}`));
  await expect(page.getByRole('heading', { name: phone.title })).toBeVisible();

  // 加入心愿单
  await page.getByRole('button', { name: '加入心愿单' }).click();
  await expect(page.getByText('已加入心愿单')).toBeVisible();

  // 验证心愿单页
  await page.getByRole('button', { name: 'Wishlist' }).click();
  await expect(page).toHaveURL(/wishlist/);
  const wishlistCard = page.locator('div').filter({ hasText: phone.title }).first();
  await expect(wishlistCard).toBeVisible();
  await page.getByRole('button', { name: '查看详情' }).first().click();
  await expect(page.getByRole('heading', { name: phone.title })).toBeVisible();

  // 加入购物车并跳转结算
  await page.getByRole('button', { name: '加入购物车' }).click();
  await expect(page.getByText('已加入购物车')).toBeVisible();
  await page.getByRole('button', { name: '去结算' }).click();

  await expect(page).toHaveURL(/checkout/);
  await expect(page.getByText(phone.title)).toBeVisible();

  // 填写收货信息
  await page.getByLabel('收件人姓名 *').fill('Playwright Buyer');
  await page.getByLabel('地址 *').fill('123 Testing Ave');
  await page.getByLabel('地址补充').fill('Suite 5');
  await page.getByLabel('城市 *').fill('Sydney');
  await page.getByLabel('州/省').fill('NSW');
  await page.getByLabel('邮编 *').fill('2000');
  await page.getByLabel('国家 *').fill('Australia');
  await page.getByLabel('联系电话').fill('0400000000');

  // 提交订单
  await page.getByRole('button', { name: '提交订单' }).click();
  await expect(page.getByText('下单成功')).toBeVisible();
  await expect(page.getByText('订单编号')).toBeVisible();
});
