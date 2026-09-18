import type { EventApi, EventsApi } from '@/api/EventsApi';

export class EventsService implements EventsApi { private readonly events = new Map<string, ClientEvent>(); of(id: string) { let event = this.events.get(id); if (!event) { event = new ClientEvent(); this.events.set(id, event); } return event; } }
class ClientEvent implements EventApi { private readonly listeners = new Set<(...args: unknown[]) => void>(); connect(callback: (...args: unknown[]) => void) { this.listeners.add(callback); return () => this.listeners.delete(callback); } fire(...args: unknown[]) { for (const callback of [...this.listeners]) callback(...args); } }
