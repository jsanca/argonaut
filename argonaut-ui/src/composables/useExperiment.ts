import { ref, reactive, readonly } from 'vue'
import type { Framework, ExperimentRequest, ExperimentResult } from '../types/argonaut'
import { ArgonautClient } from '../api/ArgonautClient'

export type RunState = 'idle' | 'running' | 'done' | 'error'

export interface FrameworkRun {
  frameworkId: string
  state: RunState
  result: ExperimentResult | null
  error: string | null
}

const runs = reactive<Map<string, FrameworkRun>>(new Map())
const selectedFrameworkId = ref<string | null>(null)

function ensureRun(frameworkId: string): FrameworkRun {
  if (!runs.has(frameworkId)) {
    runs.set(frameworkId, { frameworkId, state: 'idle', result: null, error: null })
  }
  return runs.get(frameworkId)!
}

async function runFramework(framework: Framework, request: ExperimentRequest): Promise<void> {
  const run = ensureRun(framework.id)
  run.state = 'running'
  run.result = null
  run.error = null

  const client = new ArgonautClient(framework.baseUrl)
  try {
    const result = await client.runExperiment(request)
    run.result = result
    run.state = 'done'
  } catch (e) {
    run.error = e instanceof Error ? e.message : String(e)
    run.state = 'error'
  }
}

async function runAll(frameworks: readonly Framework[], request: ExperimentRequest): Promise<void> {
  const results = await Promise.allSettled(
    frameworks.map(f => runFramework(f, request))
  )
  // errors are captured inside runFramework per framework; allSettled ensures
  // all run regardless of individual failures
  results.forEach(() => {/* outcomes handled inside runFramework */})
}

function reset(): void {
  runs.clear()
  selectedFrameworkId.value = null
}

export function useExperiment() {
  return {
    runs: readonly(runs) as ReadonlyMap<string, FrameworkRun>,
    selectedFrameworkId,
    runFramework,
    runAll,
    reset,
  }
}
