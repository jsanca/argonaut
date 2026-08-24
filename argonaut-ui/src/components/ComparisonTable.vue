<template>
  <div class="section">
    <div class="section-header">Comparison</div>
    <div class="section-body" style="padding: 0;">
      <table>
        <thead>
          <tr>
            <th>Framework</th>
            <th>Status</th>
            <th>Duration</th>
            <th>Model Calls*</th>
            <th>Tool Calls*</th>
            <th>Searches</th>
            <th>Reads</th>
            <th>Evidence</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="run in runs"
            :key="run.frameworkId"
            :class="{ selected: selectedId === run.frameworkId }"
            @click="emit('select', run.frameworkId)"
          >
            <td>{{ run.frameworkId }}</td>
            <td>
              <span v-if="run.state === 'running'">
                <span class="spinner"></span>
              </span>
              <span v-else :class="['badge', stateBadge(run.state)]">
                {{ run.state }}
              </span>
            </td>
            <td class="mono">
              <span v-if="run.result">{{ run.result.metrics.durationMs }}ms</span>
              <span v-else class="text-muted">—</span>
            </td>
            <td class="mono">
              <span v-if="run.result">{{ run.result.metrics.modelCalls }}</span>
              <span v-else class="text-muted">—</span>
            </td>
            <td class="mono">
              <span v-if="run.result">{{ run.result.metrics.toolCalls }}</span>
              <span v-else class="text-muted">—</span>
            </td>
            <td class="mono">
              <span v-if="run.result">{{ run.result.metrics.knowledgeSearches }}</span>
              <span v-else class="text-muted">—</span>
            </td>
            <td class="mono">
              <span v-if="run.result">{{ run.result.metrics.documentReads }}</span>
              <span v-else class="text-muted">—</span>
            </td>
            <td class="mono">
              <span v-if="run.result">{{ run.result.metrics.evidenceCount }}</span>
              <span v-else class="text-muted">—</span>
            </td>
          </tr>
          <tr v-if="runs.length === 0">
            <td colspan="8" class="placeholder-text" style="text-align: center; padding: 16px;">
              No experiments run yet.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p class="footnote" style="padding: 8px 16px;">
      * modelCalls and toolCalls accuracy varies across frameworks; values should not be ranked.
    </p>
  </div>
</template>

<script setup lang="ts">
import type { FrameworkRun, RunState } from '../composables/useExperiment'

defineProps<{
  runs: FrameworkRun[]
  selectedId: string | null
}>()

const emit = defineEmits<{
  select: [frameworkId: string]
}>()

function stateBadge(state: RunState): string {
  switch (state) {
    case 'done': return 'badge-run-completed'
    case 'error': return 'badge-run-failed'
    case 'running': return 'badge-run-running'
    default: return 'badge-run-idle'
  }
}
</script>
