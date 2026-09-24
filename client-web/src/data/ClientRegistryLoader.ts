import {
  validateRegistryEntries,
  type ClientRegistryEntryMap,
  type EngineRegistryName,
} from './ClientRegistrySchemas';

export interface ClientRegistryIndexResponse {
  hash: string;
  registries: Record<string, ClientRegistryIndexEntry>;
}

export interface ClientRegistryIndexEntry {
  hash: string;
  url: string;
}

export interface ClientRegistrySnapshotResponse {
  registry: string;
  hash: string;
  entries: Record<string, unknown>;
}

export interface ClientRegistrySnapshots {
  hash: string;
  registries: Record<string, ClientRegistrySnapshotResponse>;
}

export interface ClientRegistryLoaderOptions {
  serverApiUrl: string;
  authToken: string;
}

export class ClientRegistryRequestError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = 'ClientRegistryRequestError';
  }
}

export async function loadClientRegistries(options: ClientRegistryLoaderOptions): Promise<ClientRegistrySnapshots> {
  const index = parseIndex(await fetchJson(
    joinServerApiUrl(options.serverApiUrl, '/client/registries'),
    options.authToken,
    'client registry index',
  ));
  const snapshots = await Promise.all(
    Object.entries(index.registries).map(async ([registryName, registryIndexEntry]) => {
      const snapshot = parseSnapshot(await fetchJson(
        resolveServerUrl(options.serverApiUrl, registryIndexEntry.url),
        options.authToken,
        `${registryName} registry`,
      ), registryName);

      return [registryName, snapshot] as const;
    }),
  );

  const registries = Object.fromEntries(snapshots);
  const totalItems = snapshots.reduce((total, [registryName, snapshot]) => {
    const itemCount = Object.keys(snapshot.entries).length;
    console.log(`[Registries] Loaded ${registryName}: ${itemCount} items`);
    return total + itemCount;
  }, 0);

  console.log(`[Registries] Loaded ${snapshots.length} registries with ${totalItems} total items`);

  return {
    hash: index.hash,
    registries,
  };
}

export function getRegistryEntry<K extends EngineRegistryName>(
  snapshots: ClientRegistrySnapshots,
  registryName: K | `selene:${K}`,
  identifier: string,
): ClientRegistryEntryMap[K] | undefined;
export function getRegistryEntry(
  snapshots: ClientRegistrySnapshots,
  registryName: string,
  identifier: string,
): unknown;
export function getRegistryEntry(
  snapshots: ClientRegistrySnapshots,
  registryName: string,
  identifier: string,
): unknown {
  return getRegistry(snapshots, registryName)?.entries[identifier];
}

export function resolveServerUrl(serverApiUrl: string, path: string): string {
  return new URL(path, `${serverApiUrl.replace(/\/+$/, '')}/`).toString();
}

export function getRegistry(
  snapshots: ClientRegistrySnapshots,
  registryName: string,
): ClientRegistrySnapshotResponse | undefined {
  return snapshots.registries[registryName] ?? snapshots.registries[`selene:${registryName}`];
}

async function fetchJson(url: string, authToken: string, label: string): Promise<unknown> {
  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${authToken}`,
    },
  });

  if (!response.ok) {
    throw new ClientRegistryRequestError(
      `Failed to fetch ${label}: ${response.status} ${response.statusText}`,
      response.status,
    );
  }

  return response.json() as Promise<unknown>;
}

function parseIndex(value: unknown): ClientRegistryIndexResponse {
  if (!isRecord(value) || typeof value.hash !== 'string' || !isRecord(value.registries)) {
    throw new Error('Invalid client registry index.');
  }
  const registries: Record<string, ClientRegistryIndexEntry> = {};
  for (const [name, entry] of Object.entries(value.registries)) {
    if (!isRecord(entry) || typeof entry.hash !== 'string' || typeof entry.url !== 'string') {
      throw new Error(`Invalid client registry index entry: ${name}.`);
    }
    registries[name] = { hash: entry.hash, url: entry.url };
  }
  return { hash: value.hash, registries };
}

function parseSnapshot(value: unknown, requestedRegistry: string): ClientRegistrySnapshotResponse {
  if (!isRecord(value) || typeof value.registry !== 'string' || typeof value.hash !== 'string') {
    throw new Error(`Invalid ${requestedRegistry} registry snapshot.`);
  }
  if (value.registry !== requestedRegistry) {
    throw new Error(`Registry snapshot mismatch: requested ${requestedRegistry}, received ${value.registry}.`);
  }
  return {
    registry: value.registry,
    hash: value.hash,
    entries: validateRegistryEntries(value.registry, value.entries),
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function joinServerApiUrl(serverApiUrl: string, path: string): string {
  return `${serverApiUrl.replace(/\/+$/, '')}${path}`;
}
