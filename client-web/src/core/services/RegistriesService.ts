import type { IdentifierApi, RegistryObjectApi, RegistriesApi } from '@/api/RegistriesApi';
import { getRegistry, type ClientRegistrySnapshotResponse, type ClientRegistrySnapshots } from '@/data/ClientRegistryLoader';
import type { NameIdMappings } from '@/networking/NameIdMappings';
import { isRecord } from './utils';

export class RegistriesService implements RegistriesApi {
  constructor(private readonly snapshots: ClientRegistrySnapshots, private readonly mappings: NameIdMappings) {}
  add(registryName: string, identifier: string, value: unknown): RegistryObjectApi {
    const registry = this.mutableRegistry(registryName); registry.entries[identifier] = value;
    return this.wrap(registryName, identifier, value);
  }
  findAll(registry: string) { return this.entries(registry).map(([name, value]) => this.wrap(registry, name, value)); }
  findByName(registry: string, identifier: string) {
    const match = this.entries(registry).find(([name]) => name === identifier);
    return match ? this.wrap(registry, match[0], match[1]) : null;
  }
  findByMetadata(registry: string, key: string, expected: unknown) {
    const match = this.entries(registry).find(([, value]) => isRecord(value) && isRecord(value.metadata) && value.metadata[key] === expected);
    return match ? this.wrap(registry, match[0], match[1]) : null;
  }
  private entries(name: string): Array<[string, unknown]> { return Object.entries(getRegistry(this.snapshots, name)?.entries ?? {}); }
  private mutableRegistry(name: string): ClientRegistrySnapshotResponse {
    const found = getRegistry(this.snapshots, name); if (found) return found;
    const created = { registry: name, hash: 'runtime', entries: {} }; this.snapshots.registries[name] = created; return created;
  }
  private wrap(registry: string, name: string, value: unknown) {
    return new RegistryObject(name, value, this.mappings.getId(registry.replace(/^selene:/, ''), name) ?? null);
  }
}

export class RegistryObject implements RegistryObjectApi {
  [key: string]: unknown;
  private readonly definition: Record<string, unknown>;
  constructor(private readonly name: string, value: unknown, private readonly id: number | null) {
    this.definition = isRecord(value) ? value : { value }; Object.assign(this, this.definition);
  }
  getId() { return this.id; }
  getName() { return this.name; }
  getIdentifier() { return new Identifier(this.name); }
  getMetadata(key: string) { return isRecord(this.definition.metadata) ? this.definition.metadata[key] : undefined; }
  getField(key: string) { return this.definition[key]; }
  hasTag(tag: string) { return Array.isArray(this.definition.tags) && this.definition.tags.includes(tag); }
}

class Identifier implements IdentifierApi {
  private readonly namespace: string; private readonly path: string;
  constructor(private readonly identifier: string) { const i = identifier.indexOf(':'); this.namespace = i < 0 ? 'selene' : identifier.slice(0, i); this.path = i < 0 ? identifier : identifier.slice(i + 1); }
  getNamespace() { return this.namespace; } getPath() { return this.path; }
  withPrefix(prefix: string) { return new Identifier(`${this.namespace}:${prefix}${this.path}`); }
  withSuffix(suffix: string) { return new Identifier(`${this.namespace}:${this.path}${suffix}`); }
  toString() { return this.identifier; }
}
