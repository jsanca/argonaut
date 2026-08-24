<template>
  <div id="app">
    <header class="app-header">
      <h1>Argonaut</h1>
      <span class="subtitle">AI Framework Experiment Lab</span>
    </header>

    <main class="app-main">
      <ExperimentPanel
        :frameworks="frameworkList"
        :health-map="healthMap"
        @run="handleRun"
        @run-all="handleRunAll"
      />

      <HealthStatus
        :frameworks="frameworkList"
        :health-map="healthMap"
        @refresh="checkHealth"
      />

      <ComparisonTable
        :runs="runList"
        :selected-id="selectedFrameworkId"
        @select="onSelectFramework"
      />

      <div class="section">
        <div class="section-header">
          Result Detail
          <span v-if="selectedRun" class="text-secondary" style="font-weight: 400; text-transform: none; letter-spacing: 0;">
            — {{ selectedRun.frameworkId }}
          </span>
        </div>
        <ResultDetail :run="selectedRun" />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { HealthStatus as FrameworkHealthStatus } from './types/argonaut'
import { useFrameworkRegistry } from './composables/useFrameworkRegistry'
import { useExperiment } from './composables/useExperiment'
import { ArgonautClient } from './api/ArgonautClient'
import ExperimentPanel from './components/ExperimentPanel.vue'
import HealthStatus from './components/HealthStatus.vue'
import ComparisonTable from './components/ComparisonTable.vue'
import ResultDetail from './components/ResultDetail.vue'

const { frameworks, load } = useFrameworkRegistry()
const { runs, selectedFrameworkId, runFramework, runAll, reset } = useExperiment()

const healthMap = ref<Record<string, FrameworkHealthStatus>>({})

const frameworkList = computed(() => [...frameworks.value])

const runList = computed(() => {
  const list = []
  for (const run of runs.values()) {
    list.push(run)
  }
  return list
})

const selectedRun = computed(() => {
  if (!selectedFrameworkId.value) return null
  return runs.get(selectedFrameworkId.value) ?? null
})

async function checkHealth() {
  for (const framework of frameworks.value) {
    healthMap.value[framework.id] = 'checking'
  }
  await Promise.allSettled(
    frameworks.value.map(async f => {
      const client = new ArgonautClient(f.baseUrl)
      const ok = await client.health()
      healthMap.value[f.id] = ok ? 'available' : 'unavailable'
    })
  )
}

async function handleRun(selected: string[], question: string) {
  reset()
  const runId = crypto.randomUUID()
  const request = { runId, question, parameters: {} }
  const selectedFrameworks = frameworks.value.filter(f => selected.includes(f.id))
  await Promise.allSettled(selectedFrameworks.map(f => runFramework(f, request)))
}

async function handleRunAll(question: string) {
  reset()
  const runId = crypto.randomUUID()
  const request = { runId, question, parameters: {} }
  await runAll(frameworks.value, request)
}

function onSelectFramework(id: string) {
  selectedFrameworkId.value = id
}

onMounted(async () => {
  await load()
  await checkHealth()
})
</script>
