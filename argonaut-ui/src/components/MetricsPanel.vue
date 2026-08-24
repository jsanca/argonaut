<template>
  <div class="section-body">
    <dl class="metrics-list">
      <dt class="metrics-label">Duration</dt>
      <dd class="metrics-value">{{ metrics.durationMs }}ms</dd>

      <dt class="metrics-label">Model calls</dt>
      <dd class="metrics-value">
        {{ metrics.modelCalls }}
        <span class="metrics-note">accuracy varies across frameworks</span>
      </dd>

      <dt class="metrics-label">Tool calls</dt>
      <dd class="metrics-value">
        {{ metrics.toolCalls }}
        <span class="metrics-note">accuracy varies across frameworks</span>
      </dd>

      <dt class="metrics-label">Knowledge searches</dt>
      <dd class="metrics-value">{{ metrics.knowledgeSearches }}</dd>

      <dt class="metrics-label">Document reads</dt>
      <dd class="metrics-value">{{ metrics.documentReads }}</dd>

      <dt class="metrics-label">Evidence items</dt>
      <dd class="metrics-value">{{ metrics.evidenceCount }}</dd>

      <dt class="metrics-label">Errors</dt>
      <dd class="metrics-value" :class="{ 'text-error': metrics.errors > 0 }">
        {{ metrics.errors }}
      </dd>
    </dl>
  </div>
</template>

<script setup lang="ts">
import type { ExecutionMetrics } from '../types/argonaut'

defineProps<{
  metrics: ExecutionMetrics
}>()
</script>
