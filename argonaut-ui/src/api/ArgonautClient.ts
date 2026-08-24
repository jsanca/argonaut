import type { ExperimentRequest, ExperimentResult, ArgonautInfo } from '../types/argonaut'

export class ArgonautClient {
  constructor(private readonly baseUrl: string) {}

  async health(): Promise<boolean> {
    try {
      const resp = await fetch(`${this.baseUrl}/api/health`, {
        signal: AbortSignal.timeout(5000),
      })
      return resp.ok
    } catch {
      return false
    }
  }

  async about(): Promise<ArgonautInfo> {
    const resp = await fetch(`${this.baseUrl}/api/about`, {
      signal: AbortSignal.timeout(5000),
    })
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
    return resp.json() as Promise<ArgonautInfo>
  }

  async runExperiment(request: ExperimentRequest): Promise<ExperimentResult> {
    const resp = await fetch(`${this.baseUrl}/api/experiment/run`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
      signal: AbortSignal.timeout(120_000),
    })
    if (!resp.ok) {
      const body = await resp.text().catch(() => '')
      throw new Error(`HTTP ${resp.status}${body ? ': ' + body : ''}`)
    }
    return resp.json() as Promise<ExperimentResult>
  }
}
