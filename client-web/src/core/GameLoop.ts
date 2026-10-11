export type GameUpdate = (deltaMs: number, elapsedMs: number) => void;

const maxDeltaMs = 100;

export class GameLoop {
  private animationFrameId: number | null = null;
  private lastTimestamp = 0;

  constructor(private readonly update: GameUpdate) {}

  start(): void {
    if (this.animationFrameId !== null) {
      return;
    }

    this.lastTimestamp = performance.now();
    this.animationFrameId = requestAnimationFrame(this.step);
  }

  private readonly step = (timestamp: number): void => {
    const elapsedMs = Math.max(0, timestamp - this.lastTimestamp);
    const deltaMs = Math.min(elapsedMs, maxDeltaMs);
    this.lastTimestamp = timestamp;

    this.update(deltaMs, elapsedMs);
    this.animationFrameId = requestAnimationFrame(this.step);
  };
}
