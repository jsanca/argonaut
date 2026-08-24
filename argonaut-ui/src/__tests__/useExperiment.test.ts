import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { Framework, ExperimentRequest, ExperimentResult } from '../types/argonaut'

const mockFrameworkA: Framework = {
  id: 'spring-ai',
  name: 'Spring AI',
  baseUrl: 'http://localhost:8081',
  enabled: true,
}

const mockFrameworkB: Framework = {
  id: 'langchain4j',
  name: 'LangChain4j',
  baseUrl: 'http://localhost:8082',
  enabled: true,
}

const mockRequest: ExperimentRequest = {
  runId: 'run-test-1',
  question: 'Why controlled evidence?',
  parameters: {},
}

const mockResult: ExperimentResult = {
  runId: 'run-test-1',
  frameworkId: 'spring-ai',
  status: 'COMPLETED',
  finalAnswer: 'Controlled evidence ensures reproducibility.',
  evidence: [],
  trace: { events: [] },
  metrics: {
    durationMs: 800,
    modelCalls: 1,
    toolCalls: 2,
    knowledgeSearches: 1,
    documentReads: 1,
    evidenceCount: 1,
    errors: 0,
  },
  errors: [],
}

beforeEach(() => {
  vi.resetModules()
  vi.restoreAllMocks()
})

describe('useExperiment', () => {
  it('initial state has no runs', async () => {
    const { useExperiment } = await import('../composables/useExperiment')
    const { runs } = useExperiment()
    expect(runs.size).toBe(0)
  })

  it('runFramework() sets state to running then done on success', async () => {
    vi.doMock('../api/ArgonautClient', () => ({
      ArgonautClient: vi.fn().mockImplementation(() => ({
        runExperiment: vi.fn().mockResolvedValue(mockResult),
      })),
    }))
    const { useExperiment } = await import('../composables/useExperiment')
    const { runs, runFramework } = useExperiment()
    await runFramework(mockFrameworkA, mockRequest)
    const run = runs.get('spring-ai')
    expect(run).toBeDefined()
    expect(run!.state).toBe('done')
    expect(run!.result).toEqual(mockResult)
    expect(run!.error).toBeNull()
  })

  it('runFramework() sets state to error on API failure', async () => {
    vi.doMock('../api/ArgonautClient', () => ({
      ArgonautClient: vi.fn().mockImplementation(() => ({
        runExperiment: vi.fn().mockRejectedValue(new Error('HTTP 503')),
      })),
    }))
    const { useExperiment } = await import('../composables/useExperiment')
    const { runs, runFramework } = useExperiment()
    await runFramework(mockFrameworkA, mockRequest)
    const run = runs.get('spring-ai')
    expect(run).toBeDefined()
    expect(run!.state).toBe('error')
    expect(run!.error).toContain('HTTP 503')
    expect(run!.result).toBeNull()
  })

  it('runAll() runs all frameworks independently', async () => {
    vi.doMock('../api/ArgonautClient', () => ({
      ArgonautClient: vi.fn().mockImplementation(() => ({
        runExperiment: vi.fn().mockResolvedValue(mockResult),
      })),
    }))
    const { useExperiment } = await import('../composables/useExperiment')
    const { runs, runAll } = useExperiment()
    await runAll([mockFrameworkA, mockFrameworkB], mockRequest)
    expect(runs.has('spring-ai')).toBe(true)
    expect(runs.has('langchain4j')).toBe(true)
  })

  it('runAll() completes even when one framework fails', async () => {
    let callCount = 0
    vi.doMock('../api/ArgonautClient', () => ({
      ArgonautClient: vi.fn().mockImplementation(() => ({
        runExperiment: vi.fn().mockImplementation(() => {
          callCount++
          if (callCount === 1) return Promise.reject(new Error('HTTP 500'))
          return Promise.resolve(mockResult)
        }),
      })),
    }))
    const { useExperiment } = await import('../composables/useExperiment')
    const { runs, runAll } = useExperiment()
    await runAll([mockFrameworkA, mockFrameworkB], mockRequest)
    // Both frameworks should have a run entry (one error, one done)
    expect(runs.has('spring-ai')).toBe(true)
    expect(runs.has('langchain4j')).toBe(true)
    const failed = runs.get('spring-ai')
    const succeeded = runs.get('langchain4j')
    expect(failed!.state).toBe('error')
    expect(succeeded!.state).toBe('done')
  })

  it('reset() clears all run state', async () => {
    vi.doMock('../api/ArgonautClient', () => ({
      ArgonautClient: vi.fn().mockImplementation(() => ({
        runExperiment: vi.fn().mockResolvedValue(mockResult),
      })),
    }))
    const { useExperiment } = await import('../composables/useExperiment')
    const { runs, runFramework, reset } = useExperiment()
    await runFramework(mockFrameworkA, mockRequest)
    expect(runs.size).toBe(1)
    reset()
    expect(runs.size).toBe(0)
  })
})
