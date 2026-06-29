import { ref } from 'vue'

const isFullscreen = ref(false)

export function useFullscreen() {
  return { isFullscreen }
}
