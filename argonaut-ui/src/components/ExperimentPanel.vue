<template>
  <div class="section">
    <div class="section-header">UC-001 Controlled Local Evidence</div>
    <div class="section-body">
      <div class="form-group">
        <label for="question-input">Question</label>
        <textarea
          id="question-input"
          v-model="question"
          rows="3"
          placeholder="Enter your experiment question…"
        ></textarea>
      </div>

      <div class="form-group">
        <label>Frameworks</label>
        <div class="checkbox-list">
          <label
            v-for="framework in frameworks"
            :key="framework.id"
            :class="['checkbox-item', { disabled: isUnavailable(framework.id) }]"
          >
            <input
              type="checkbox"
              :value="framework.id"
              v-model="selected"
              :disabled="isUnavailable(framework.id)"
            />
            {{ framework.name }}
          </label>
        </div>
      </div>

      <div class="btn-row">
        <button
          class="btn btn-primary"
          :disabled="!canRunSelected"
          @click="handleRunSelected"
        >
          Run Selected
        </button>
        <button
          class="btn"
          :disabled="!canRunAll"
          @click="handleRunAll"
        >
          Run All
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Framework, HealthStatus } from '../types/argonaut'

const UC001_QUESTION =
  'Why does Argonaut use controlled local evidence rather than immediately introducing web search, vector databases, or external observability tools?'

const props = defineProps<{
  frameworks: Framework[]
  healthMap: Record<string, HealthStatus>
}>()

const emit = defineEmits<{
  run: [selected: string[], question: string]
  runAll: [question: string]
}>()

const question = ref(UC001_QUESTION)
const selected = ref<string[]>(props.frameworks.map(f => f.id))

function isUnavailable(id: string): boolean {
  return props.healthMap[id] === 'unavailable'
}

const canRunSelected = computed(() =>
  question.value.trim().length > 0 && selected.value.length > 0
)

const canRunAll = computed(() => question.value.trim().length > 0)

function handleRunSelected() {
  emit('run', [...selected.value], question.value.trim())
}

function handleRunAll() {
  emit('runAll', question.value.trim())
}
</script>
