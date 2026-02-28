import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

import { AbortController as NodeAbortController, AbortSignal as NodeAbortSignal } from 'node:abort_controller'

Object.defineProperty(globalThis, 'AbortController', {
  value: NodeAbortController,
  configurable: true,
  writable: true,
})

Object.defineProperty(globalThis, 'AbortSignal', {
  value: NodeAbortSignal,
  configurable: true,
  writable: true,
})

if (typeof window !== 'undefined') {
  Object.defineProperty(window, 'AbortController', {
    value: NodeAbortController,
    configurable: true,
    writable: true,
  })

  Object.defineProperty(window, 'AbortSignal', {
    value: NodeAbortSignal,
    configurable: true,
    writable: true,
  })
}

afterEach(() => {
  cleanup()
})
