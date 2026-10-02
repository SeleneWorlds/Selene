import { unzip } from 'fflate';
import { validateRegistryEntries } from '@/data/ClientRegistrySchemas';

const DATABASE_NAME = 'selene-client-mods';
const DATABASE_VERSION = 2;
const FILE_STORE = 'mod-files';
const META_STORE = 'metadata';
const MOD_LIST_KEY = 'installed-mods';

interface StoredAsset { modId: string; path: string; blob: Blob }
export interface ModInfo { id: string; name: string; fileCount: number }
export interface ModInstallProgress { label: string; progress: number }

let database: IDBDatabase | null = null;
let installedMods: ModInfo[] = [];
const pathsByMod = new Map<string, Set<string>>();

export async function initializeModAssets(): Promise<void> {
  database = await openDatabase();
  const transaction = database.transaction([FILE_STORE, META_STORE], 'readonly');
  const [storedMods, storedKeys] = await Promise.all([
    request<unknown>(transaction.objectStore(META_STORE).get(MOD_LIST_KEY)),
    request<IDBValidKey[]>(transaction.objectStore(FILE_STORE).getAllKeys()),
  ]);
  installedMods = sanitizeModList(storedMods);
  pathsByMod.clear();
  for (const key of storedKeys) {
    if (!Array.isArray(key) || typeof key[0] !== 'string' || typeof key[1] !== 'string') continue;
    let modPaths = pathsByMod.get(key[0]);
    if (!modPaths) { modPaths = new Set(); pathsByMod.set(key[0], modPaths); }
    modPaths.add(key[1]);
  }
  installedMods = installedMods.map((mod) => ({ ...mod, fileCount: pathsByMod.get(mod.id)?.size ?? 0 }));
}

export function getInstalledMods(): ModInfo[] { return installedMods.map((mod) => ({ ...mod })); }
export function hasModAsset(path: string): boolean {
  const normalized = normalizeAssetPath(path);
  return installedMods.some((mod) => pathsByMod.get(mod.id)?.has(normalized));
}
export function listModAssetPaths(): string[] {
  return [...new Set(installedMods.flatMap((mod) => [...(pathsByMod.get(mod.id) ?? [])]))];
}

export async function getModAsset(path: string): Promise<Blob | null> {
  const normalized = normalizeAssetPath(path);
  for (const mod of installedMods) {
    if (!pathsByMod.get(mod.id)?.has(normalized)) continue;
    const stored = await getStoredAsset(mod.id, normalized);
    if (stored) return stored.blob;
  }
  return null;
}

export async function applyModRegistryOverrides<T extends { registries: Record<string, { entries: Record<string, unknown> }> }>(snapshots: T): Promise<T> {
  const registries = structuredClone(snapshots.registries);
  const changed = new Set<string>();
  let applied = 0;
  // Lower-priority mods apply first, allowing the topmost mod to overwrite them.
  for (const mod of [...installedMods].reverse()) {
    const overrides = [...(pathsByMod.get(mod.id) ?? [])]
      .map((path) => ({ path, match: /^(?:common|client)\/data\/([^/]+)\/([^/]+)(?:\/(.+))?\.json$/.exec(path) }))
      .filter((entry): entry is { path: string; match: RegExpExecArray } => entry.match !== null)
      .sort((a, b) => a.path.localeCompare(b.path));
    for (const { path, match } of overrides) {
      const [, namespace, registryName, entryName] = match;
      const registryKey = findRegistryKey(registries, registryName);
      if (!registryKey) continue;
      const stored = await getStoredAsset(mod.id, path);
      if (!stored) continue;
      let parsed: unknown;
      try { parsed = JSON.parse(await stored.blob.text()); }
      catch (error) {
        console.warn(`Could not parse mod registry file ${path}.`, error);
        throw new Error(`Mod registry file is not valid JSON: ${path}`);
      }
      if (entryName) {
        registries[registryKey].entries[`${namespace}:${entryName}`] = parsed;
        changed.add(registryKey); applied += 1;
      } else {
        if (!isRecord(parsed) || !isRecord(parsed.entries)) throw new Error(`Mod registry file must contain an entries object: ${path}`);
        for (const [name, value] of Object.entries(parsed.entries)) {
          registries[registryKey].entries[`${namespace}:${name}`] = value;
          changed.add(registryKey); applied += 1;
        }
      }
    }
  }
  for (const name of changed) registries[name].entries = validateRegistryEntries(name, registries[name].entries);
  if (applied) console.info(`[Mods] Applied ${applied} local registry overrides from ${installedMods.length} mod(s).`);
  return { ...snapshots, registries };
}

export async function installMod(file: File, onProgress?: (progress: ModInstallProgress) => void): Promise<ModInfo> {
  if (!database) throw new Error('Browser mod storage is not available.');
  let archive: Record<string, Uint8Array>;
  try {
    onProgress?.({ label: 'Reading ZIP', progress: 0 });
    const bytes = await readFileWithProgress(file, (p) => onProgress?.({ label: 'Reading ZIP', progress: p * 0.2 }));
    onProgress?.({ label: 'Extracting files', progress: 0.2 });
    archive = await unzipArchive(bytes);
    onProgress?.({ label: 'Preparing files', progress: 0.4 });
  } catch (error) {
    console.warn('Could not read mod ZIP.', error);
    throw new Error('The selected file is not a valid ZIP archive.');
  }
  const entries = Object.entries(archive).filter(([path]) => !path.endsWith('/'))
    .map(([path, contents]) => ({ path: normalizeAssetPath(path), contents }))
    .filter((entry) => entry.path && !entry.path.startsWith('__MACOSX/'));
  if (!entries.length) throw new Error('The ZIP archive does not contain any files.');
  const root = findArchiveRoot(entries.map((entry) => entry.path), file.name);
  const id = crypto.randomUUID();
  const assets = entries.map(({ path, contents }) => ({ modId: id, path: root ? path.slice(root.length + 1) : path, blob: new Blob([new Uint8Array(contents).buffer]) })).filter(({ path }) => path);
  const mod = { id, name: file.name, fileCount: assets.length };
  const nextMods = [mod, ...installedMods];
  const transaction = database.transaction([FILE_STORE, META_STORE], 'readwrite');
  let stored = 0; let lastPercent = -1;
  for (const asset of assets) {
    const storing = transaction.objectStore(FILE_STORE).put(asset);
    storing.onsuccess = () => {
      const progress = 0.4 + (++stored / assets.length) * 0.59;
      const percent = Math.floor(progress * 100);
      if (percent !== lastPercent) { lastPercent = percent; onProgress?.({ label: `Storing files (${stored.toLocaleString()} of ${assets.length.toLocaleString()})`, progress }); }
    };
  }
  transaction.objectStore(META_STORE).put(nextMods, MOD_LIST_KEY);
  await transactionDone(transaction);
  installedMods = nextMods;
  pathsByMod.set(id, new Set(assets.map((asset) => asset.path)));
  onProgress?.({ label: 'Finishing installation', progress: 1 });
  return { ...mod };
}

export async function removeMod(id: string): Promise<void> {
  if (!database || !installedMods.some((mod) => mod.id === id)) return;
  const next = installedMods.filter((mod) => mod.id !== id);
  const transaction = database.transaction([FILE_STORE, META_STORE], 'readwrite');
  for (const path of pathsByMod.get(id) ?? []) transaction.objectStore(FILE_STORE).delete([id, path]);
  transaction.objectStore(META_STORE).put(next, MOD_LIST_KEY);
  await transactionDone(transaction);
  installedMods = next; pathsByMod.delete(id);
}

export async function moveMod(id: string, direction: -1 | 1): Promise<ModInfo[]> {
  if (!database) return getInstalledMods();
  const index = installedMods.findIndex((mod) => mod.id === id);
  const target = index + direction;
  if (index < 0 || target < 0 || target >= installedMods.length) return getInstalledMods();
  const next = [...installedMods];
  [next[index], next[target]] = [next[target], next[index]];
  const transaction = database.transaction(META_STORE, 'readwrite');
  transaction.objectStore(META_STORE).put(next, MOD_LIST_KEY);
  await transactionDone(transaction);
  installedMods = next;
  return getInstalledMods();
}

export function normalizeAssetPath(path: string): string {
  const normalized = path.replace(/\\/g, '/').replace(/^\/+/, '').replace(/^\.\//, '');
  return !normalized || normalized.split('/').some((part) => part === '..') ? '' : normalized;
}

async function getStoredAsset(modId: string, path: string): Promise<StoredAsset | undefined> {
  if (!database) return undefined;
  return request(database.transaction(FILE_STORE, 'readonly').objectStore(FILE_STORE).get([modId, path]));
}

function sanitizeModList(value: unknown): ModInfo[] {
  return Array.isArray(value) ? value.filter((mod): mod is ModInfo => isRecord(mod) && typeof mod.id === 'string' && typeof mod.name === 'string' && typeof mod.fileCount === 'number').map((mod) => ({ ...mod })) : [];
}
async function readFileWithProgress(file: File, report: (progress: number) => void): Promise<Uint8Array> {
  const reader = file.stream().getReader(); const result = new Uint8Array(file.size); let offset = 0;
  while (true) { const { done, value } = await reader.read(); if (done) break; result.set(value, offset); offset += value.byteLength; report(file.size ? offset / file.size : 1); }
  return result;
}
function unzipArchive(bytes: Uint8Array): Promise<Record<string, Uint8Array>> { return new Promise((resolve, reject) => unzip(bytes, (error, archive) => error ? reject(error) : resolve(archive))); }
function findArchiveRoot(paths: string[], fileName: string): string | null { const root = paths[0]?.split('/')[0]; const name = fileName.replace(/\.zip$/i, ''); return root && root.toLowerCase() === name.toLowerCase() && paths.every((path) => path.startsWith(`${root}/`)) ? root : null; }
function findRegistryKey(registries: Record<string, unknown>, name: string): string | null { return name in registries ? name : `selene:${name}` in registries ? `selene:${name}` : null; }
function isRecord(value: unknown): value is Record<string, unknown> { return typeof value === 'object' && value !== null && !Array.isArray(value); }

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const opening = indexedDB.open(DATABASE_NAME, DATABASE_VERSION);
    opening.onupgradeneeded = () => {
      const db = opening.result;
      if (db.objectStoreNames.contains('files')) db.deleteObjectStore('files');
      if (!db.objectStoreNames.contains(FILE_STORE)) db.createObjectStore(FILE_STORE, { keyPath: ['modId', 'path'] });
      if (!db.objectStoreNames.contains(META_STORE)) db.createObjectStore(META_STORE);
    };
    opening.onsuccess = () => resolve(opening.result);
    opening.onerror = () => reject(opening.error ?? new Error('Could not open browser mod storage.'));
  });
}
function request<T>(value: IDBRequest<T>): Promise<T> { return new Promise((resolve, reject) => { value.onsuccess = () => resolve(value.result); value.onerror = () => reject(value.error ?? new Error('Browser storage request failed.')); }); }
function transactionDone(transaction: IDBTransaction): Promise<void> { return new Promise((resolve, reject) => { transaction.oncomplete = () => resolve(); transaction.onerror = () => reject(transaction.error ?? new Error('Browser storage transaction failed.')); transaction.onabort = () => reject(transaction.error ?? new Error('Browser storage transaction was aborted.')); }); }
