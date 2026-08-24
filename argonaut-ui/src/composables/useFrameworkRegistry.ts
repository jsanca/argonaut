import { ref, readonly } from 'vue'
import type { Framework } from '../types/argonaut'

const frameworks = ref<Framework[]>([])
const loaded = ref(false)
const error = ref<string | null>(null)

async function load(): Promise<void> {
  if (loaded.value) return
  try {
    const resp = await fetch('/config/frameworks.json')
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
    const data = await resp.json() as { frameworks: Framework[] }
    frameworks.value = data.frameworks.filter(f => f.enabled)
    loaded.value = true
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  }
}

export function useFrameworkRegistry() {
  return { frameworks: readonly(frameworks), loaded: readonly(loaded), error: readonly(error), load }
}
