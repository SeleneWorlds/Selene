import type {
  EntitiesApi,
  ClientEntitiesListener,
  EntityApi,
  ClientEntityDefinition,
  ClientEntityScriptRunner,
  ClientEntitySnapshot,
} from '@/api/EntitiesApi';
import { getRegistryEntry, type ClientRegistrySnapshots } from '@/data/ClientRegistryLoader';
import type { Coordinate, EntityPacket, GamePacket } from '@/networking/GameProtocol';
import type { NameIdMappings } from '@/networking/NameIdMappings';

export class ClientEntities implements EntitiesApi {
  private readonly entitiesById = new Map<number, ClientEntity>();
  private readonly listeners = new Set<ClientEntitiesListener>();
  private readonly scriptedEntities = new Set<ClientEntity>();
  private nextLocalId = -1;

  constructor(
    private readonly registries: ClientRegistrySnapshots,
    private readonly nameIdMappings: NameIdMappings,
  ) {}

  create(entityDefinition: string | ClientEntityDefinition): EntityApi {
    const { entityName, definition } = this.resolveEntityDefinition(entityDefinition);
    const entity = new ClientEntity(
      this.nextLocalId,
      entityName,
      definition,
      (snapshot) => this.emitEntityChanged(snapshot),
      (id) => this.emitEntityRemoved(id),
      (id, alpha) => this.emitEntityVisualAlphaChanged(id, alpha),
      (scriptedEntity, isScripted) => this.updateScriptedEntity(scriptedEntity, isScripted),
    );

    this.nextLocalId -= 1;
    this.entitiesById.set(entity.id, entity);
    return entity;
  }

  addListener(listener: ClientEntitiesListener): () => void {
    this.listeners.add(listener);

    return () => {
      this.listeners.delete(listener);
    };
  }

  getEntitiesAt(coordinate: Coordinate): EntityApi[] {
    return [...this.entitiesById.values()].filter((entity) => entity.spawned && isSameCoordinate(entity.coordinate, coordinate));
  }

  getEntityByNetworkId(networkId: number): EntityApi | null {
    return this.entitiesById.get(networkId) ?? null;
  }

  findEntitiesAt(coordinate: Coordinate, criteria?: { tag?: string }): EntityApi[] {
    return this.getEntitiesAt(coordinate).filter((entity) => {
      if (!criteria?.tag) {
        return true;
      }

      return entity.getDefinition().tags?.includes(criteria.tag) ?? false;
    });
  }

  tickClientScripts(deltaSeconds: number, runScript: ClientEntityScriptRunner): void {
    for (const entity of [...this.scriptedEntities]) {
      if (!entity.isClientScriptActive()) {
        continue;
      }
      const scriptModule = entity.getClientScriptModule();
      if (scriptModule) {
        runScript(scriptModule, entity.id, entity, deltaSeconds);
      }
    }
  }

  handlePacket(packet: GamePacket): void {
    switch (packet.type) {
      case 'entity':
        this.upsertNetworkEntity(packet);
        break;
      case 'moveEntity':
        this.entitiesById.get(packet.networkId)?.applyNetworkMove(packet.end, packet.facing);
        break;
      case 'turnEntity':
        this.entitiesById.get(packet.networkId)?.applyNetworkFacing(packet.facing);
        break;
      case 'removeEntity':
        this.entitiesById.delete(packet.networkId);
        break;
      default:
        break;
    }
  }

  private upsertNetworkEntity(packet: EntityPacket): void {
    const entityName = this.nameIdMappings.getName('entities', packet.entityId);
    if (!entityName) {
      return;
    }

    const definition = getRegistryEntry(this.registries, 'entities', entityName);
    if (!definition) {
      return;
    }

    const existing = this.entitiesById.get(packet.networkId);
    if (existing) {
      existing.applyNetworkState(packet.coordinate, packet.facing, packet.components);
      return;
    }

    const entity = new ClientEntity(
      packet.networkId,
      entityName,
      definition,
      (snapshot) => this.emitEntityChanged(snapshot),
      (id) => this.emitEntityRemoved(id),
      (id, alpha) => this.emitEntityVisualAlphaChanged(id, alpha),
      (scriptedEntity, isScripted) => this.updateScriptedEntity(scriptedEntity, isScripted),
      packet.networkId,
    );
    entity.applyNetworkState(packet.coordinate, packet.facing, packet.components);
    entity.applyNetworkSpawn();
    this.entitiesById.set(packet.networkId, entity);
  }

  private resolveEntityDefinition(entityDefinition: string | ClientEntityDefinition): {
    entityName: string;
    definition: ClientEntityDefinition;
  } {
    if (typeof entityDefinition !== 'string') {
      return {
        entityName: 'client:anonymous',
        definition: entityDefinition,
      };
    }

    const definition = getRegistryEntry(this.registries, 'entities', entityDefinition);
    if (!definition) {
      throw new Error(`Unknown entity definition: ${entityDefinition}`);
    }

    return { entityName: entityDefinition, definition };
  }

  private emitEntityChanged(snapshot: ClientEntitySnapshot): void {
    for (const listener of this.listeners) {
      listener.entityChanged(snapshot);
    }
  }

  private emitEntityRemoved(id: number): void {
    const entity = this.entitiesById.get(id);
    if (entity) {
      this.scriptedEntities.delete(entity);
      this.entitiesById.delete(id);
    }

    for (const listener of this.listeners) {
      listener.entityRemoved(id);
    }
  }

  private emitEntityVisualAlphaChanged(id: number, alpha: number): void {
    for (const listener of this.listeners) {
      listener.entityVisualAlphaChanged(id, alpha);
    }
  }

  private updateScriptedEntity(entity: ClientEntity, isScripted: boolean): void {
    if (isScripted) {
      this.scriptedEntities.add(entity);
    } else {
      this.scriptedEntities.delete(entity);
    }
  }
}

class ClientEntity implements EntityApi {
  coordinate: Coordinate = { x: 0, y: 0, z: 0 };
  facing = 0;
  spawned = false;
  private readonly components: Record<string, unknown>;
  private clientScriptModule: string | null;

  constructor(
    readonly id: number,
    private readonly entityName: string,
    private readonly definition: ClientEntityDefinition,
    private readonly onChanged: (snapshot: ClientEntitySnapshot) => void,
    private readonly onRemoved: (id: number) => void,
    private readonly onVisualAlphaChanged: (id: number, alpha: number) => void,
    private readonly onScriptedChanged: (entity: ClientEntity, isScripted: boolean) => void,
    private readonly networkId = 0,
  ) {
    this.components = { ...(definition.components ?? {}) };
    this.clientScriptModule = findClientScriptModule(this.components);
  }

  getNetworkId(): number {
    return this.networkId;
  }

  getCoordinate(): Coordinate {
    return { ...this.coordinate };
  }

  getFacing(): number {
    return this.facing;
  }

  getDefinitionName(): string {
    return this.entityName;
  }

  spawn(): void {
    this.spawned = true;
    this.updateScriptedState();
    this.notifyChanged();
  }

  despawn(): void {
    this.spawned = false;
    this.updateScriptedState();
    this.onRemoved(this.id);
  }

  setCoordinate(coordinate: Coordinate): void {
    this.coordinate = { ...coordinate };
    if (this.spawned) {
      this.notifyChanged();
    }
  }

  setFacing(facing: number): void {
    this.facing = facing;
    if (this.spawned) {
      this.notifyChanged();
    }
  }

  addComponent(name: string, componentData: unknown): void {
    this.components[name] = componentData;
    this.clientScriptModule = findClientScriptModule(this.components);
    this.updateScriptedState();
    if (this.spawned) {
      this.notifyChanged();
    }
  }

  setComponentAlpha(name: string, alpha: number): void {
    const component = this.components[name];
    if (!isRecord(component)) {
      return;
    }
    this.components[name] = { ...component, alpha };
    if (this.spawned && component.type === 'visual') {
      this.onVisualAlphaChanged(this.id, Math.max(0, Math.min(1, alpha)));
    } else if (this.spawned) {
      this.notifyChanged();
    }
  }

  setComponentProperty(name: string, property: string, value: unknown): void {
    const component = this.components[name];
    if (!isRecord(component)) return;
    this.components[name] = { ...component, [property]: value };
    if (this.spawned) this.notifyChanged();
  }

  getComponent(name: string): unknown {
    return this.components[name] ?? null;
  }

  getDefinition(): ClientEntityDefinition {
    return this.definition;
  }

  getClientScriptModule(): string | null {
    return this.clientScriptModule;
  }

  isClientScriptActive(): boolean {
    return this.spawned && this.networkId === 0 && this.clientScriptModule !== null;
  }

  private updateScriptedState(): void {
    this.onScriptedChanged(
      this,
      this.isClientScriptActive(),
    );
  }

  private snapshot(): ClientEntitySnapshot {
    return {
      id: this.id,
      networkId: this.networkId,
      entityName: this.entityName,
      definition: this.definition,
      coordinate: this.getCoordinate(),
      facing: this.facing,
      components: { ...this.components },
      spawned: this.spawned,
    };
  }

  applyNetworkState(coordinate: Coordinate, facing: number, components: Record<string, unknown>): void {
    this.coordinate = { ...coordinate };
    this.facing = facing;
    Object.assign(this.components, components);
  }

  applyNetworkMove(coordinate: Coordinate, facing: number): void {
    this.coordinate = { ...coordinate };
    this.facing = facing;
  }

  applyNetworkFacing(facing: number): void {
    this.facing = facing;
  }

  applyNetworkSpawn(): void {
    this.spawned = true;
  }

  private notifyChanged(): void {
    this.onChanged(this.snapshot());
  }
}

function isSameCoordinate(left: Coordinate, right: Coordinate): boolean {
  return left.x === right.x && left.y === right.y && left.z === right.z;
}

function isClientScriptComponent(value: unknown): value is { type: string; script: string } {
  return (
    typeof value === 'object'
    && value !== null
    && (value as { type?: unknown }).type === 'client_script'
    && typeof (value as { script?: unknown }).script === 'string'
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function findClientScriptModule(components: Record<string, unknown>): string | null {
  const scriptComponent = Object.values(components).find(isClientScriptComponent);
  return scriptComponent?.script ?? null;
}
