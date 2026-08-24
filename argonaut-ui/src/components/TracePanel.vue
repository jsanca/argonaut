<template>
  <div class="section-body">
    <p v-if="events.length === 0" class="placeholder-text">No trace events recorded.</p>
    <div v-else>
      <div class="trace-timeline">
        <div v-for="event in events" :key="event.id" class="trace-event">
          <div class="trace-timestamp">{{ formatTimestamp(event.timestamp) }}</div>
          <div class="trace-event-meta">
            <span :class="['badge', badgeClass(event.type)]">{{ event.type }}</span>
            <div class="trace-event-summary">{{ event.summary }}</div>
            <div v-if="hasMetadata(event)" class="trace-event-kv">
              <span
                v-for="(value, key) in event.metadata"
                :key="key"
                class="trace-kv-pair"
              >
                <span class="trace-kv-key">{{ key }}</span>=<span>{{ truncate(value, 60) }}</span>
              </span>
            </div>
          </div>
          <div v-if="event.durationMs !== null" class="trace-timestamp">
            {{ event.durationMs }}ms
          </div>
        </div>
      </div>

      <div class="raw-json-section">
        <button class="raw-json-toggle" @click="showRaw = !showRaw">
          {{ showRaw ? 'Hide raw JSON' : 'Show raw JSON' }}
        </button>
        <pre v-if="showRaw" class="raw-json-pre">{{ JSON.stringify(events, null, 2) }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ExecutionEvent } from '../types/argonaut'

defineProps<{
  events: ExecutionEvent[]
}>()

const showRaw = ref(false)

function badgeClass(type: string): string {
  if (type.startsWith('RUN_')) return 'badge-blue'
  if (type.startsWith('KNOWLEDGE_')) return 'badge-green'
  if (type.startsWith('DOCUMENT_')) return 'badge-teal'
  if (type.startsWith('EVIDENCE_')) return 'badge-purple'
  if (type === 'ANSWER_SYNTHESIZED') return 'badge-orange'
  if (type.startsWith('MODEL_CALL_')) return 'badge-yellow'
  if (type.startsWith('TOOL_CALL_')) return 'badge-red'
  return 'badge-gray'
}

function hasMetadata(event: ExecutionEvent): boolean {
  return Object.keys(event.metadata).length > 0
}

function formatTimestamp(ts: string): string {
  try {
    return new Date(ts).toISOString().substring(11, 23)
  } catch {
    return ts
  }
}

function truncate(s: string, max: number): string {
  return s.length > max ? s.substring(0, max) + '…' : s
}
</script>
