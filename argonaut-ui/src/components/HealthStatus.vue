<template>
  <div class="section">
    <div class="section-header">
      <span>Service Status</span>
      <button class="btn" @click="emit('refresh')">Refresh</button>
    </div>
    <div class="section-body">
      <div class="health-row">
        <div
          v-for="framework in frameworks"
          :key="framework.id"
          class="health-item"
        >
          <div :class="['health-dot', dotClass(healthMap[framework.id] ?? 'checking')]"></div>
          <span>{{ framework.name }}</span>
          <span :class="['badge', badgeClass(healthMap[framework.id] ?? 'checking')]">
            {{ healthMap[framework.id] ?? 'checking' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Framework, HealthStatus } from '../types/argonaut'

defineProps<{
  frameworks: Framework[]
  healthMap: Record<string, HealthStatus>
}>()

const emit = defineEmits<{
  refresh: []
}>()

function dotClass(status: HealthStatus): string {
  return `health-dot-${status}`
}

function badgeClass(status: HealthStatus): string {
  return `badge-status-${status}`
}
</script>
