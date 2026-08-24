import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ArgonautClient } from '../api/ArgonautClient'
import type { ArgonautInfo, ExperimentRequest, ExperimentResult } from '../types/argonaut'

const BASE_URL = 'http://localhost:8081'

const mockInfo: ArgonautInfo = {
  frameworkId: 'spring-ai',
  frameworkName: 'Spring AI',
  implementationVersion: '0.1.0-SNAPSHOT',
  modelProvider: 'openrouter',
  modelName: 'openai/gpt-4o-mini',
  capabilities: ['RAG'],
}

const mockResult: ExperimentResult = {
  runId: 'run-1',
  frameworkId: 'spring-ai',
  status: 'COMPLETED',
  finalAnswer: 'Because controlled evidence ensures reproducibility.',
  evidence: [],
  trace: { events: [] },
  metrics: {
    durationMs: 1200,
    modelCalls: 1,
    toolCalls: 2,
    knowledgeSearches: 1,
    documentReads: 1,
    evidenceCount: 1,
    errors: 0,
  },
  errors: [],
}

const mockRequest: ExperimentRequest = {
  runId: 'run-1',
  question: 'Why controlled evidence?',
  parameters: {},
}

function makeFetch(status: number, body: unknown): typeof fetch {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body)),
  } as Response)
}

function makeFailingFetch(error: Error): typeof fetch {
  return vi.fn().mockRejectedValue(error)
}

beforeEach(() => {
  vi.restoreAllMocks()
})

describe('ArgonautClient.health()', () => {
  it('returns true when server responds 200 OK', async () => {
    vi.stubGlobal('fetch', makeFetch(200, { status: 'UP' }))
    const client = new ArgonautClient(BASE_URL)
    expect(await client.health()).toBe(true)
  })

  it('returns false on network error', async () => {
    vi.stubGlobal('fetch', makeFailingFetch(new Error('network error')))
    const client = new ArgonautClient(BASE_URL)
    expect(await client.health()).toBe(false)
  })

  it('returns false on non-OK status (503)', async () => {
    vi.stubGlobal('fetch', makeFetch(503, { status: 'DOWN' }))
    const client = new ArgonautClient(BASE_URL)
    expect(await client.health()).toBe(false)
  })
})

describe('ArgonautClient.about()', () => {
  it('returns parsed ArgonautInfo on success', async () => {
    vi.stubGlobal('fetch', makeFetch(200, mockInfo))
    const client = new ArgonautClient(BASE_URL)
    const info = await client.about()
    expect(info.frameworkId).toBe('spring-ai')
    expect(info.frameworkName).toBe('Spring AI')
  })

  it('throws on non-OK status', async () => {
    vi.stubGlobal('fetch', makeFetch(503, {}))
    const client = new ArgonautClient(BASE_URL)
    await expect(client.about()).rejects.toThrow('HTTP 503')
  })
})

describe('ArgonautClient.runExperiment()', () => {
  it('sends POST with correct headers and body', async () => {
    const mockFetch = makeFetch(200, mockResult)
    vi.stubGlobal('fetch', mockFetch)
    const client = new ArgonautClient(BASE_URL)
    await client.runExperiment(mockRequest)

    expect(mockFetch).toHaveBeenCalledWith(
      `${BASE_URL}/api/experiment/run`,
      expect.objectContaining({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(mockRequest),
      })
    )
  })

  it('returns parsed ExperimentResult on success', async () => {
    vi.stubGlobal('fetch', makeFetch(200, mockResult))
    const client = new ArgonautClient(BASE_URL)
    const result = await client.runExperiment(mockRequest)
    expect(result.runId).toBe('run-1')
    expect(result.status).toBe('COMPLETED')
    expect(result.metrics.durationMs).toBe(1200)
  })

  it('throws on non-OK status', async () => {
    vi.stubGlobal('fetch', makeFetch(500, 'Internal Server Error'))
    const client = new ArgonautClient(BASE_URL)
    await expect(client.runExperiment(mockRequest)).rejects.toThrow('HTTP 500')
  })
})
