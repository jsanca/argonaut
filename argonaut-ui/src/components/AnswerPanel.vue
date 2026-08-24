<template>
  <div class="section-body">
    <p v-if="answer === null" class="placeholder-text">No answer available.</p>
    <p v-else class="answer-text">{{ answer }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  answer: string | null
}>()
</script>
