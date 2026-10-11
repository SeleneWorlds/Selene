import { setChunkTimingEnabled, startChunkTimingCapture, finishChunkTimingCapture,
  chunkTiming as logChunkTiming, waitForChunkWorkIdle } from '@/core/ChunkTiming';

export interface ChunkBenchmarkCamera {
  /** The caller owns zoom, server requests and any input locking. */
  zoom: number;
  getPosition(): Promise<{ x: number; y: number }>;
  move(position: { x: number; y: number }): Promise<void>;
}

/** Kept for a future client diagnostics menu; no UI invokes this runner. */
export async function runChunkBenchmark(
  camera: ChunkBenchmarkCamera, signal?: AbortSignal,
): Promise<void> {
  if (!Number.isFinite(camera.zoom) || camera.zoom <= 0) {
    throw new Error('Benchmark zoom must be positive');
  }
  signal?.throwIfAborted();
  const origin = await camera.getPosition();
  const previousTiming = setChunkTimingEnabled(true);
  const settle = async () => {
    signal?.throwIfAborted();
    if (!await waitForChunkWorkIdle(30_000)) {
      throw new Error('Chunk work did not settle within 30 seconds');
    }
    signal?.throwIfAborted();
  };
  try {
    startChunkTimingCapture();
    await settle();
    const width = window.innerWidth;
    const startedAt = performance.now();
    logChunkTiming('benchmark-start', {
      origin, zoom: camera.zoom, screenWidth: width, screenHeight: window.innerHeight,
      panWorldDistance: width / camera.zoom, durationMs: 500, benchmarkVersion: 7,
    });
    let progress = 0;
    while (progress < 1) {
      await new Promise<void>(resolve => requestAnimationFrame(() => resolve()));
      signal?.throwIfAborted();
      progress = Math.min(1, (performance.now() - startedAt) / 500);
      await camera.move({ x: origin.x + width * progress / camera.zoom, y: origin.y });
    }
    logChunkTiming('benchmark-pan-end', { panMs: performance.now() - startedAt });
    await settle();
    logChunkTiming('benchmark-end', { elapsedMs: performance.now() - startedAt });
  } catch (error) {
    logChunkTiming('benchmark-error', { message: String(error) });
    throw error;
  } finally {
    try {
      await camera.move(origin);
    } finally {
      try {
        downloadChunkTiming(finishChunkTimingCapture());
      } finally {
        setChunkTimingEnabled(previousTiming);
      }
    }
  }
}

function downloadChunkTiming(logs: string): void {
  const url = URL.createObjectURL(new Blob([logs], { type: 'application/x-ndjson' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `selene-chunk-benchmark-${new Date().toISOString().replace(/[:.]/g, '-')}.jsonl`;
  try {
    document.body.appendChild(link);
    link.click();
  } finally {
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }
}
