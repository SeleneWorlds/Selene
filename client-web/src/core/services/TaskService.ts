import type { TaskApi } from '@/api/TaskApi';

export class TaskService implements TaskApi { launch(callback: (...args: unknown[]) => void, args: unknown[]) { queueMicrotask(() => callback(...args)); } }
