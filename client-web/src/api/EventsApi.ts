export interface SignalApi { connect(callback: (...args: unknown[]) => void): (() => void) | void }
export interface EventApi extends SignalApi { fire(...args: unknown[]): void }
export interface EventsApi { of(identifier: string): EventApi }
