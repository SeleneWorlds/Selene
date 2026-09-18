import type { SchedulesApi } from '@/api/SchedulesApi';

export class SchedulesService implements SchedulesApi {
  private readonly timeouts = new Set<number>(); private readonly intervals = new Set<number>();
  readonly everySecond = this.periodic(1_000); readonly everyMinute = this.periodic(60_000); readonly everyHour = this.periodic(3_600_000);
  setTimeout(ms: number, callback: () => void) { const id = window.setTimeout(() => { this.timeouts.delete(id); callback(); }, ms); this.timeouts.add(id); return id; }
  clearTimeout(id: number) { window.clearTimeout(id); this.timeouts.delete(id); }
  setInterval(ms: number, callback: () => void, immediate: boolean) { if (immediate) callback(); const id = window.setInterval(callback, ms); this.intervals.add(id); return id; }
  clearInterval(id: number) { window.clearInterval(id); this.intervals.delete(id); }
  private periodic(ms: number) { return { connect: (callback: (...args: unknown[]) => void) => { const id = this.setInterval(ms, callback, false); return () => this.clearInterval(id); } }; }
}
