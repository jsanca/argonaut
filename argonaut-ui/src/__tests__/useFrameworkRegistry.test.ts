import { describe, it, expect, vi, beforeEach } from 'vitest'

const REGISTRY_JSON = {
  frameworks: [
    { id: 'spring-ai', name: 'Spring AI', baseUrl: 'http://localhost:8081', enabled: true },
    { id: 'langchain4j', name: 'LangChain4j', baseUrl: 'http://localhost:8082', enabled: true },
    { id: 'disabled-fw', name: 'Disabled', baseUrl: 'http://localhost:9999', enabled: false },
  ],
}

function makeFetch(status: number, body: unknown): typeof fetch {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response)
}

beforeEach(() => {
  vi.resetModules()
  vi.restoreAllMocks()
})

describe('useFrameworkRegistry', () => {
  it('load() populates frameworks from JSON', async () => {
    vi.stubGlobal('fetch', makeFetch(200, REGISTRY_JSON))
    const { useFrameworkRegistry } = await import('../composables/useFrameworkRegistry')
    const { frameworks, load } = useFrameworkRegistry()
    await load()
    expect(frameworks.value.length).toBeGreaterThan(0)
  })

  it('load() filters out disabled frameworks', async () => {
    vi.stubGlobal('fetch', makeFetch(200, REGISTRY_JSON))
    const { useFrameworkRegistry } = await import('../composables/useFrameworkRegistry')
    const { frameworks, load } = useFrameworkRegistry()
    await load()
    expect(frameworks.value.every(f => f.enabled)).toBe(true)
    expect(frameworks.value.find(f => f.id === 'disabled-fw')).toBeUndefined()
  })

  it('load() sets loaded to true after success', async () => {
    vi.stubGlobal('fetch', makeFetch(200, REGISTRY_JSON))
    const { useFrameworkRegistry } = await import('../composables/useFrameworkRegistry')
    const { loaded, load } = useFrameworkRegistry()
    await load()
    expect(loaded.value).toBe(true)
  })

  it('load() is idempotent — second call does not re-fetch', async () => {
    const mockFetch = makeFetch(200, REGISTRY_JSON)
    vi.stubGlobal('fetch', mockFetch)
    const { useFrameworkRegistry } = await import('../composables/useFrameworkRegistry')
    const { load } = useFrameworkRegistry()
    await load()
    await load()
    expect(mockFetch).toHaveBeenCalledTimes(1)
  })

  it('load() sets error on fetch failure', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network down')))
    const { useFrameworkRegistry } = await import('../composables/useFrameworkRegistry')
    const { error, load } = useFrameworkRegistry()
    await load()
    expect(error.value).toContain('network down')
  })
})
