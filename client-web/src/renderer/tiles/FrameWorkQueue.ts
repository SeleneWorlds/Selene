type WorkKey = string | symbol | object;
type Work = { task: () => void };

export interface FrameWorkStats {
  processed: number;
  completions: number;
  pending: number;
  pendingCompletions: number;
  limitReached: boolean;
}

/** Coalesced work with amortized constant-time dequeue and fair completion priority. */
export class FrameWorkQueue {
  private readonly tasks = new Map<WorkKey, Work>();
  private construction: WorkKey[] = [];
  private completion: WorkKey[] = [];
  private constructionHead = 0;
  private completionHead = 0;
  private completionBurst = 0;

  enqueue(key: WorkKey, task: () => void, completion = false): void {
    const existing = this.tasks.get(key);
    if (existing) {
      existing.task = task;
      return;
    }
    this.tasks.set(key, { task });
    (completion ? this.completion : this.construction).push(key);
  }

  process(budgetMs = 3, maxTasks = 4096, now: () => number = () => performance.now()): FrameWorkStats {
    const startedAt = now();
    const deadline = startedAt + budgetMs;
    let processed = 0;
    let completions = 0;
    while (this.tasks.size > 0 && processed < maxTasks && (processed === 0 || now() < deadline)) {
      const complete = this.completionHead < this.completion.length &&
        (this.completionBurst < 3 || this.constructionHead === this.construction.length);
      const key = complete ? this.completion[this.completionHead++] : this.construction[this.constructionHead++];
      const work = this.tasks.get(key)!;
      this.tasks.delete(key);
      this.completionBurst = complete ? this.completionBurst + 1 : 0;
      if (complete) completions += 1;
      work.task();
      processed += 1;
    }
    // Compact only occasionally, rather than scanning deleted Map entries for every job.
    if ((this.constructionHead >= 4096 && this.constructionHead * 2 >= this.construction.length) || this.constructionHead === this.construction.length) {
      this.construction = this.construction.slice(this.constructionHead);
      this.constructionHead = 0;
    }
    if ((this.completionHead >= 4096 && this.completionHead * 2 >= this.completion.length) || this.completionHead === this.completion.length) {
      this.completion = this.completion.slice(this.completionHead);
      this.completionHead = 0;
    }
    const endedAt = now();
    return {
      processed, completions,
      pending: this.tasks.size, pendingCompletions: this.completion.length - this.completionHead,
      limitReached: this.tasks.size > 0 && (processed >= maxTasks || endedAt >= deadline),
    };
  }
}
