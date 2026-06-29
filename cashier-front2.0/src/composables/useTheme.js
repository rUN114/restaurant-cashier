import { ref, watch } from 'vue'

const isDark = ref(localStorage.getItem('theme') === 'dark')

const apply = (dark) => {
  document.documentElement.classList.toggle('dark', dark)
  localStorage.setItem('theme', dark ? 'dark' : 'light')
  // Element Plus 暗色模式需要设置 data-theme
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
}

watch(isDark, apply, { immediate: true })

export function useTheme() {
  const toggle = () => { isDark.value = !isDark.value }
  return { isDark, toggle }
}
