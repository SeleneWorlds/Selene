interface Transition {
  from: number;
  to: number;
  duration: number;
  elapsed: number;
}

/** Numeric playback updates act like implicit linear keyframes, with time in seconds. */
export class TimelineParameters {
  readonly values: Record<string, unknown>;
  private readonly transitions = new Map<string, Transition>();

  constructor(initial: Readonly<Record<string, unknown>>) {
    this.values = { ...initial };
  }

  retarget(parameters: Readonly<Record<string, unknown>>, duration: number): void {
    if (!Number.isFinite(duration) || duration < 0) throw new Error('transition must be a non-negative finite number');
    for (const [name, target] of Object.entries(parameters)) {
      const from = this.values[name];
      if (duration > 0 && typeof target === 'number' && this.transitions.get(name)?.to === target) continue;
      this.transitions.delete(name);
      if (duration > 0 && typeof from === 'number' && Number.isFinite(from)
        && typeof target === 'number' && Number.isFinite(target) && from !== target) {
        this.transitions.set(name, { from, to: target, duration, elapsed: 0 });
      } else {
        this.values[name] = target;
      }
    }
  }

  update(delta: number): void {
    for (const [name, transition] of this.transitions) {
      transition.elapsed = Math.min(transition.duration, transition.elapsed + delta);
      const progress = transition.elapsed / transition.duration;
      this.values[name] = transition.from + (transition.to - transition.from) * progress;
      if (progress >= 1) this.transitions.delete(name);
    }
  }
}
