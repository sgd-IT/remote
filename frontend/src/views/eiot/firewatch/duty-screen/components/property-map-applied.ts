type AppliedRecord = { expiresAt: number; renderedPropertyIds: Set<string> }
type AppliedWaiter = {
  snapshotId: string
  pointId: string | null
  resolve: (value: boolean) => void
  timer: ReturnType<typeof setTimeout>
}

/**
 * Tracks map acknowledgements independently from request timing. The iframe may
 * acknowledge a snapshot before the detail verification request completes.
 */
export function createPropertyMapAppliedTracker(ttlMs = 30_000, maxRecords = 64) {
  const records = new Map<string, AppliedRecord>()
  const waiters = new Set<AppliedWaiter>()
  const matches = (record: AppliedRecord, pointId: string | null) => pointId === null || record.renderedPropertyIds.has(pointId)
  const prune = () => {
    const now = Date.now()
    for (const [snapshotId, record] of records) if (record.expiresAt <= now) records.delete(snapshotId)
    while (records.size > maxRecords) records.delete(records.keys().next().value as string)
  }
  const mark = (snapshotId: string, renderedPropertyIds: string[]) => {
    prune()
    const record = { expiresAt: Date.now() + ttlMs, renderedPropertyIds: new Set(renderedPropertyIds) }
    records.delete(snapshotId)
    records.set(snapshotId, record)
    for (const waiter of [...waiters]) {
      if (waiter.snapshotId !== snapshotId || !matches(record, waiter.pointId)) continue
      clearTimeout(waiter.timer)
      waiters.delete(waiter)
      waiter.resolve(true)
    }
  }
  const wait = (snapshotId: string, pointId: string | null, timeoutMs = 2_000) => {
    prune()
    const received = records.get(snapshotId)
    if (received && matches(received, pointId)) return Promise.resolve(true)
    return new Promise<boolean>((resolve) => {
      const waiter: AppliedWaiter = {
        snapshotId,
        pointId,
        resolve,
        timer: setTimeout(() => { waiters.delete(waiter); resolve(false) }, timeoutMs),
      }
      waiters.add(waiter)
    })
  }
  const clear = () => {
    records.clear()
    for (const waiter of waiters) { clearTimeout(waiter.timer); waiter.resolve(false) }
    waiters.clear()
  }
  return { mark, wait, clear }
}
