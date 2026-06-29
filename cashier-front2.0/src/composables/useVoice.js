let enabled = localStorage.getItem('voice') !== 'off'

export function useVoice() {
  const speak = (text) => {
    if (!enabled || !window.speechSynthesis) return
    window.speechSynthesis.cancel()
    const u = new SpeechSynthesisUtterance(text)
    u.lang = 'zh-CN'
    u.rate = 1.1
    u.pitch = 1
    u.volume = 0.8
    window.speechSynthesis.speak(u)
  }

  const toggle = () => {
    enabled = !enabled
    localStorage.setItem('voice', enabled ? 'on' : 'off')
    return enabled
  }

  const isOn = () => enabled

  return { speak, toggle, isOn }
}
