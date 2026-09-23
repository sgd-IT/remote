const CHANNEL = 'firewatch-property-map'
const STORAGE_KEY = 'firewatch:property-map-change'

/** The notification contains no property data; receivers always reload their authorized list. */
export function announcePropertyMapChange(): void {
  window.dispatchEvent(new Event('property-map-saved'))
  const version = `${Date.now()}-${Math.random()}`
  try {
    const channel = new BroadcastChannel(CHANNEL)
    channel.postMessage(version)
    channel.close()
  } catch { /* BroadcastChannel is optional. */ }
  try { window.localStorage.setItem(STORAGE_KEY, version) } catch { /* Polling remains the fallback. */ }
}

export function subscribePropertyMapChanges(onChange: () => void): () => void {
  const onSaved = () => onChange()
  const onStorage = (event: StorageEvent) => { if (event.key === STORAGE_KEY && event.newValue) onChange() }
  let channel: BroadcastChannel | null = null
  try {
    channel = new BroadcastChannel(CHANNEL)
    channel.onmessage = () => onChange()
  } catch { /* Polling remains the fallback. */ }
  window.addEventListener('property-map-saved', onSaved)
  window.addEventListener('storage', onStorage)
  return () => {
    window.removeEventListener('property-map-saved', onSaved)
    window.removeEventListener('storage', onStorage)
    channel?.close()
  }
}
