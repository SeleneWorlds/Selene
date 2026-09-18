import type { SignalApi } from './EventsApi';
export interface SchedulesApi { readonly everySecond: SignalApi; readonly everyMinute: SignalApi; readonly everyHour: SignalApi; setTimeout(milliseconds: number, callback: () => void): number; clearTimeout(id: number): void; setInterval(milliseconds: number, callback: () => void, immediate: boolean): number; clearInterval(id: number): void }
