<template>
  <div>
    <div v-if="!run || run.state === 'idle'" class="section-body">
      <p class="placeholder-text">Select a framework row above to view result detail.</p>
    </div>

    <div v-else-if="run.state === 'running'" class="section-body">
      <p class="text-secondary">
        <span class="spinner" style="margin-right: 8px;"></span>
        Running experiment for <strong>{{ run.frameworkId }}</strong>…
      </p>
    </div>

    <div v-else-if="run.state === 'error'" class="section-body">
      <p class="text-error">Error: {{ run.error }}</p>
    </div>

    <div v-else-if="run.state === 'done' && run.result">
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          :class="['tab-btn', { active: activeTab === tab.id }]"
          @click="activeTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>

      <AnswerPanel
        v-if="activeTab === 'answer'"
        :answer="run.result.finalAnswer"
      />
      <EvidencePanel
        v-else-if="activeTab === 'evidence'"
        :evidence="run.result.evidence"
      />
      <TracePanel
        v-else-if="activeTab === 'trace'"
        :events="run.result.trace.events"
      />
      <MetricsPanel
        v-else-if="activeTab === 'metrics'"
        :metrics="run.result.metrics"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FrameworkRun } from '../composables/useExperiment'
import AnswerPanel from './AnswerPanel.vue'
import EvidencePanel from './EvidencePanel.vue'
import TracePanel from './TracePanel.vue'
import MetricsPanel from './MetricsPanel.vue'

defineProps<{
  run: FrameworkRun | null
}>()

const activeTab = ref<'answer' | 'evidence' | 'trace' | 'metrics'>('answer')

const tabs = [
  { id: 'answer' as const, label: 'Answer' },
  { id: 'evidence' as const, label: 'Evidence' },
  { id: 'trace' as const, label: 'Trace' },
  { id: 'metrics' as const, label: 'Metrics' },
]
</script>
