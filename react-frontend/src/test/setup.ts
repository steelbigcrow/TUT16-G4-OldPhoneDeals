import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'
import { transferableAbortController } from 'node:util'

// Node 25 在 globalThis 上预置了无实现的 localStorage / sessionStorage 桩对象，
// 会覆盖 jsdom 的真实实现，导致 clear/setItem 等方法缺失。这里把 jsdom 的实现装回全局。
const jsdomWindow = (globalThis as { window?: Window }).window

if (jsdomWindow) {
  const jsdomLocalStorage = (jsdomWindow as unknown as { _localStorage?: Storage })._localStorage
  const jsdomSessionStorage = (jsdomWindow as unknown as { _sessionStorage?: Storage })._sessionStorage

  for (const [key, storage] of [
    ['localStorage', jsdomLocalStorage],
    ['sessionStorage', jsdomSessionStorage],
  ] as const) {
    if (!storage) continue
    for (const target of [globalThis, jsdomWindow]) {
      Object.defineProperty(target, key, {
        value: storage,
        configurable: true,
        writable: true,
      })
    }
  }
}

// jsdom 的 AbortSignal 与 Node（undici）的 Request 不是同一实现，
// 会导致 RequestInit 校验失败。这里统一为 Node 的 transferable 实现。
const NodeAbortController = transferableAbortController().constructor as typeof AbortController
const NodeAbortSignal = transferableAbortController().signal.constructor as typeof AbortSignal

for (const target of [globalThis, jsdomWindow]) {
  if (!target) continue
  Object.defineProperty(target, 'AbortController', {
    value: NodeAbortController,
    configurable: true,
    writable: true,
  })
  Object.defineProperty(target, 'AbortSignal', {
    value: NodeAbortSignal,
    configurable: true,
    writable: true,
  })
}

afterEach(() => {
  cleanup()
})
