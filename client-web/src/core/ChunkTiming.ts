/** Enable with localStorage.setItem('selene.chunkTiming', '1'), then reload. */
export let chunkTimingEnabled = (() => {
  try { return typeof localStorage !== 'undefined' && localStorage.getItem('selene.chunkTiming') === '1'; }
  catch { return false; }
})();

let capture: string[] | null = null;
let dropped = 0;
const maxEntries = 50_000;

export function chunkTiming(stage: string, details: object): void {
  if (!chunkTimingEnabled) return;
  if (stage === 'chunk-enqueued' || stage === 'packet') lastChunkActivityAt = performance.now();
  const entry = { stage, atMs: performance.now(), ...details };
  if (capture) {
    if (capture.length < maxEntries) capture.push(JSON.stringify(entry));
    else dropped += 1;
  } else {
    console.info(`[chunk-timing] ${stage}`, entry);
  }
}

export function startChunkTimingCapture(): void {
  capture = [];
  dropped = 0;
  // Allow a frame to report existing work before an initial idle check succeeds.
  lastChunkActivityAt = performance.now();
  chunkTiming('capture-start', {
    date: new Date().toISOString(), timeOrigin: performance.timeOrigin,
    userAgent: navigator.userAgent,
  });
}

/** JSON Lines preserves event order and snapshots values when they are logged. */
export function finishChunkTimingCapture(): string {
  const entries = capture ?? [];
  capture = null;
  entries.push(JSON.stringify({ stage: 'capture-end', atMs: performance.now(), droppedEntries: dropped }));
  return entries.join('\n') + '\n';
}

/** Returns the previous setting so temporary diagnostics can restore it. */
export function setChunkTimingEnabled(enabled: boolean): boolean {
  const previous = chunkTimingEnabled;
  chunkTimingEnabled = enabled;
  return previous;
}

let queuedChunkWork = 0;
let pendingStacks = 0;
let lastChunkActivityAt = 0;

export function reportChunkWork(queued: number, pending: number): void {
  queuedChunkWork = queued;
  pendingStacks = pending;
}

/** Background animation jobs do not prevent the benchmark from settling. */
export async function waitForChunkWorkIdle(timeoutMs = 30_000): Promise<boolean> {
  const startedAt = performance.now();
  while (performance.now() - startedAt < timeoutMs) {
    if (queuedChunkWork === 0 && pendingStacks === 0 && performance.now() - lastChunkActivityAt >= 500) return true;
    await new Promise<void>(resolve => setTimeout(resolve, 100));
  }
  return false;
}
