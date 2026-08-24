export interface Framework {
  id: string
  name: string
  baseUrl: string
  enabled: boolean
}

export interface FrameworkRegistry {
  frameworks: Framework[]
}

export interface ExperimentRequest {
  runId: string
  question: string
  parameters: Record<string, string>
}

export interface Evidence {
  id: string
  kind: string
  sourceId: string
  title: string | null
  excerpt: string | null
  score: number
  usedFor: string | null
}

export interface ExecutionEvent {
  id: string
  timestamp: string
  type: string
  actor: string
  name: string
  summary: string
  metadata: Record<string, string>
  durationMs: number | null
}

export interface ExecutionTrace {
  events: ExecutionEvent[]
}

export interface ExecutionMetrics {
  durationMs: number
  modelCalls: number
  toolCalls: number
  knowledgeSearches: number
  documentReads: number
  evidenceCount: number
  errors: number
}

export interface ArgonautError {
  code: string
  message: string
}

export interface ExperimentResult {
  runId: string
  frameworkId: string
  status: 'COMPLETED' | 'FAILED' | 'PARTIAL'
  finalAnswer: string | null
  evidence: Evidence[]
  trace: ExecutionTrace
  metrics: ExecutionMetrics
  errors: ArgonautError[]
}

export interface ArgonautInfo {
  frameworkId: string
  frameworkName: string
  implementationVersion: string
  modelProvider: string | null
  modelName: string | null
  capabilities: string[]
}

export type HealthStatus = 'available' | 'unavailable' | 'checking'
