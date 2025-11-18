const DEFAULT_MEDIA_BASE =
  process.env.NEXT_PUBLIC_FILES_BASE_URL ?? 'http://localhost:3000';

export function buildMediaUrl(path?: string): string | undefined {
  if (!path) {
    return undefined;
  }

  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }

  if (path.startsWith('/')) {
    return `${DEFAULT_MEDIA_BASE}${path}`;
  }

  return `${DEFAULT_MEDIA_BASE}/${path}`;
}
