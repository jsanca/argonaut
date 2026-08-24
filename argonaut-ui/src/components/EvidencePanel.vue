<template>
  <div class="section-body">
    <p v-if="evidence.length === 0" class="placeholder-text">No evidence recorded.</p>
    <table v-else>
      <thead>
        <tr>
          <th>ID</th>
          <th>Source</th>
          <th>Kind</th>
          <th>Score</th>
          <th>Excerpt</th>
          <th>Used For</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in evidence" :key="item.id">
          <td class="mono">{{ item.id }}</td>
          <td class="mono">{{ item.sourceId }}</td>
          <td>
            <span class="badge badge-gray">{{ item.kind }}</span>
          </td>
          <td class="mono">{{ item.score.toFixed(3) }}</td>
          <td>
            <span class="evidence-excerpt" :title="item.excerpt ?? ''">
              {{ item.excerpt ?? '—' }}
            </span>
          </td>
          <td class="text-secondary">{{ item.usedFor ?? '—' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { Evidence } from '../types/argonaut'

defineProps<{
  evidence: Evidence[]
}>()
</script>
