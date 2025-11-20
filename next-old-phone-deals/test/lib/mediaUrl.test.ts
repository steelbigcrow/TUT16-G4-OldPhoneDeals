describe('buildMediaUrl', () => {
  const originalBase = process.env.NEXT_PUBLIC_FILES_BASE_URL;

  afterEach(() => {
    process.env.NEXT_PUBLIC_FILES_BASE_URL = originalBase;
    jest.resetModules();
  });

  it('returns undefined when path is missing', () => {
    jest.isolateModules(() => {
      const { buildMediaUrl } = require('@/lib/mediaUrl');
      expect(buildMediaUrl()).toBeUndefined();
    });
  });

  it('returns absolute urls unchanged', () => {
    jest.isolateModules(() => {
      const { buildMediaUrl } = require('@/lib/mediaUrl');
      expect(buildMediaUrl('https://cdn.com/a.png')).toBe('https://cdn.com/a.png');
      expect(buildMediaUrl('http://cdn.com/a.png')).toBe('http://cdn.com/a.png');
    });
  });

  it('uses env base for relative paths', () => {
    process.env.NEXT_PUBLIC_FILES_BASE_URL = 'https://files.example.com';
    jest.isolateModules(() => {
      const { buildMediaUrl } = require('@/lib/mediaUrl');
      expect(buildMediaUrl('/imgs/pic.png')).toBe('https://files.example.com/imgs/pic.png');
      expect(buildMediaUrl('imgs/pic.png')).toBe('https://files.example.com/imgs/pic.png');
    });
  });

  it('falls back to localhost when env is unset', () => {
    delete process.env.NEXT_PUBLIC_FILES_BASE_URL;
    jest.isolateModules(() => {
      const { buildMediaUrl } = require('@/lib/mediaUrl');
      expect(buildMediaUrl('/asset.png')).toBe('http://localhost:3000/asset.png');
    });
  });
});
