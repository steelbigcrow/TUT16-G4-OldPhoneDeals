export function resolveImageUrl(image: string | null | undefined): string {
  const value = (image ?? '').trim()
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  if (value.startsWith('/')) return value
  return `/${value}`
}

