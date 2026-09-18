const BROKER_PATH = '/storage-broker.html';
const BROKER_TIMEOUT_MS = 5_000;

type StorageRequest =
  | { type: 'storage-load'; key: string }
  | { type: 'storage-save'; key: string; value: string };

interface StorageResponse {
  id: number;
  ok: boolean;
  error?: unknown;
  value?: unknown;
}

export interface StorageBroker {
  load(key: string): Promise<string | null>;
  save(key: string, value: string): Promise<void>;
}

let storageBroker: StorageBroker | null = null;

export function getStorageBroker(): StorageBroker {
  storageBroker ??= createStorageBroker();
  return storageBroker;
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function createStorageBroker(): StorageBroker {
  if (isLocalhost(window.location.hostname)) return new LocalStorageBroker();

  const rootDomain = import.meta.env.VITE_SELENE_BASE_DOMAIN?.trim().replace(/^\./, '');
  if (!rootDomain) throw new Error('Missing required storage configuration: VITE_SELENE_BASE_DOMAIN.');
  if (!isDomainOrSubdomain(window.location.hostname, rootDomain)) {
    throw new Error(`Storage is not available outside ${rootDomain}.`);
  }

  return new RemoteStorageBroker(`https://settings.${rootDomain}`);
}

class LocalStorageBroker implements StorageBroker {
  async load(key: string): Promise<string | null> {
    return window.localStorage.getItem(key);
  }

  async save(key: string, value: string): Promise<void> {
    window.localStorage.setItem(key, value);
  }
}

class RemoteStorageBroker implements StorageBroker {
  private readonly ready: Promise<MessagePort>;
  private requestId = 0;

  constructor(private readonly origin: string) {
    this.ready = this.connect();
  }

  async load(key: string): Promise<string | null> {
    const response = await this.request({ type: 'storage-load', key });
    return typeof response.value === 'string' ? response.value : null;
  }

  async save(key: string, value: string): Promise<void> {
    await this.request({ type: 'storage-save', key, value });
  }

  private connect(): Promise<MessagePort> {
    return new Promise((resolve, reject) => {
      const iframe = document.createElement('iframe');
      iframe.hidden = true;
      iframe.title = 'Selene storage broker';
      iframe.src = `${this.origin}${BROKER_PATH}`;

      const timeout = window.setTimeout(() => {
        iframe.remove();
        reject(new Error('Storage broker connection timed out.'));
      }, BROKER_TIMEOUT_MS);

      iframe.addEventListener('load', () => {
        const channel = new MessageChannel();
        channel.port1.addEventListener('message', (event: MessageEvent<unknown>) => {
          if (!isRecord(event.data) || event.data.type !== 'ready') return;
          window.clearTimeout(timeout);
          resolve(channel.port1);
        }, { once: true });
        channel.port1.start();
        iframe.contentWindow?.postMessage({ type: 'selene-storage-connect' }, this.origin, [channel.port2]);
      }, { once: true });

      iframe.addEventListener('error', () => {
        window.clearTimeout(timeout);
        iframe.remove();
        reject(new Error('Could not load the storage broker.'));
      }, { once: true });

      document.body.append(iframe);
    });
  }

  private async request(request: StorageRequest): Promise<StorageResponse & { ok: true }> {
    const port = await this.ready;
    const id = ++this.requestId;

    return new Promise((resolve, reject) => {
      const timeout = window.setTimeout(() => {
        port.removeEventListener('message', handleResponse);
        reject(new Error('Storage broker request timed out.'));
      }, BROKER_TIMEOUT_MS);

      const handleResponse = (event: MessageEvent<unknown>): void => {
        if (!isStorageResponse(event.data, id)) return;
        window.clearTimeout(timeout);
        port.removeEventListener('message', handleResponse);
        if (event.data.ok) {
          resolve(event.data as StorageResponse & { ok: true });
        } else {
          reject(new Error(typeof event.data.error === 'string' ? event.data.error : 'Storage broker request failed.'));
        }
      };

      port.addEventListener('message', handleResponse);
      port.postMessage({ ...request, id });
    });
  }
}

function isStorageResponse(value: unknown, id: number): value is StorageResponse {
  return isRecord(value) && value.id === id && typeof value.ok === 'boolean';
}

function isLocalhost(hostname: string): boolean {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '[::1]';
}

function isDomainOrSubdomain(hostname: string, domain: string): boolean {
  return hostname === domain || hostname.endsWith(`.${domain}`);
}
