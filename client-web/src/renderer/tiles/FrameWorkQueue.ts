type WorkKey = string | symbol | object;
type Work = { key: WorkKey; task: () => void; priority: number; previous?: Work | undefined; next?: Work | undefined };
type WorkList = { head?: Work | undefined; tail?: Work | undefined; count: number };

export interface FrameWorkStats {
  constructionMs: number;
  removalMs: number;
  completionMs: number;
  backgroundMs: number;
  processed: number;
  completions: number;
  removals: number;
  pendingRemovals: number;
  pending: number;
  pendingCompletions: number;
  pendingBackground: number;
  limitReached: boolean;
}

/** Spend more of the frame filling missing tiles; cleanup keeps the smaller budget. */
export function frameWorkBudget(pending: number, elapsedMs: number, incoming = 0): number {
  if (incoming >= 512) return 12;
  if (pending >= 8192) return 8;
  if (pending <= 512 || !Number.isFinite(elapsedMs)) return 3;
  return Math.min(8, Math.max(3, elapsedMs / 4));
}

/** Coalesced work with constant-time promotion and fair foreground priorities. */
export class FrameWorkQueue {
  private readonly tasks = new Map<WorkKey, Work>();
  private readonly queues: WorkList[] = Array.from({ length: 4 }, () => ({ count: 0 }));
  private completionBurst = 0;


  get pendingCount(): number { return this.tasks.size; }
  get pendingIncomingCount(): number { return this.queues[0].count + this.queues[1].count; }

  enqueue(key: WorkKey, task: () => void, completion = false, background = false): void {
    this.enqueueAt(key, task, background ? 3 : completion ? 0 : 1);
  }

  enqueueRemoval(key: WorkKey, task: () => void): void {
    this.enqueueAt(key, task, 2);
  }

  private enqueueAt(key: WorkKey, task: () => void, priority: number): void {
    const existing = this.tasks.get(key);
    if (existing?.priority === priority) {
      existing.task = task;
      return;
    }
    if (existing) this.unlink(existing);
    const work: Work = { key, task, priority };
    const queue = this.queues[priority];
    work.previous = queue.tail;
    if (queue.tail) queue.tail.next = work;
    else queue.head = work;
    queue.tail = work;
    queue.count += 1;
    this.tasks.set(key, work);
  }

  private unlink(work: Work): void {
    const queue = this.queues[work.priority];
    if (work.previous) work.previous.next = work.next;
    else queue.head = work.next;
    if (work.next) work.next.previous = work.previous;
    else queue.tail = work.previous;
    queue.count -= 1;
  }

  process(budgetMs = 3, maxTasks = 4096, now: () => number = () => performance.now(), measureCosts = false): FrameWorkStats {
    const deadline = now() + budgetMs;
    let processed = 0;
    let completions = 0;
    let removals = 0;
    const costs = [0, 0, 0, 0];
    while (this.tasks.size > 0 && processed < maxTasks && (processed === 0 || now() < deadline)) {
      const [completion, construction, removal] = this.queues;
      let priority: number;
      if (completion.count && (this.completionBurst < 3 || !construction.count)) {
        priority = 0;
      } else if (construction.count) {
        priority = 1;
      } else if (removal.count) {
        priority = 2;
      } else {
        priority = 3;
      }
      const work = this.queues[priority].head!;
      this.unlink(work);
      this.tasks.delete(work.key);
      this.completionBurst = priority === 0 ? this.completionBurst + 1 : 0;
      if (priority === 0) completions += 1;
      if (priority === 2) removals += 1;
      const taskStartedAt = measureCosts ? now() : 0;
      work.task();
      if (measureCosts) costs[priority] += now() - taskStartedAt;
      processed += 1;
    }
    return {
      processed, completions, removals,
      completionMs: costs[0], constructionMs: costs[1], removalMs: costs[2], backgroundMs: costs[3],
      pendingBackground: this.queues[3].count,
      pendingRemovals: this.queues[2].count,
      pending: this.tasks.size, pendingCompletions: this.queues[0].count,
      limitReached: this.tasks.size > 0 && (processed >= maxTasks || now() >= deadline),
    };
  }
}
