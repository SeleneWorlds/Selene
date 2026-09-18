import { TILE_STEP_X, TILE_STEP_Y, TILE_STEP_Z } from '@/core/WorldProjection';
import { getRegistryEntry, type ClientRegistrySnapshots } from '@/data/ClientRegistryLoader';
import type {
  ClientDirection,
  GridApi,
  ClientGridDirectionDefinition,
} from '@/api/GridApi';
import type { Coordinate } from '@/networking/GameProtocol';

export class ClientGrid implements GridApi {
  private readonly directions = new Map<string, ClientDirection>();

  constructor(private readonly registries: ClientRegistrySnapshots) {}

  applyGrid(identifier: string): void {
    const definition = getRegistryEntry(this.registries, 'grids', identifier);
    if (!definition) {
      throw new Error(`Unknown grid: ${identifier}`);
    }
    if (!Array.isArray(definition.directions)) {
      throw new Error(`Grid ${identifier} does not define directions.`);
    }

    const directions = definition.directions.map((direction) => {
      const directionName = requireDirectionName(direction, identifier);

      return {
        name: directionName,
        x: requireInteger(direction.x, identifier, directionName, 'x'),
        y: requireInteger(direction.y, identifier, directionName, 'y'),
        z: requireInteger(direction.z, identifier, directionName, 'z'),
        angle: requireFiniteNumber(direction.angle, identifier, directionName, 'angle'),
      };
    });

    this.directions.clear();
    for (const direction of directions) {
      this.defineDirection(direction.name, direction.x, direction.y, direction.z, direction.angle);
    }
  }

  defineDirection(name: string, x: number, y: number, z: number, angle: number): Coordinate {
    const vector = { x, y, z };

    this.directions.set(name, {
      name,
      vector,
      angle,
    });

    return { ...vector };
  }

  getDirectionByName(name: string): ClientDirection {
    const direction = this.directions.get(name);

    if (!direction) {
      throw new Error(`Unknown direction: ${name}`);
    }

    return {
      name: direction.name,
      vector: { ...direction.vector },
      angle: direction.angle,
    };
  }

  getDirection(angle: number): ClientDirection | null {
    let closest: ClientDirection | null = null;
    let closestDistance = Number.POSITIVE_INFINITY;

    for (const direction of this.directions.values()) {
      // Facing angles wrap at 360 degrees.
      const difference = Math.abs(((direction.angle - angle + 180) % 360 + 360) % 360 - 180);
      if (difference < closestDistance) {
        closest = direction;
        closestDistance = difference;
      }
    }

    return closest ? { ...closest, vector: { ...closest.vector } } : null;
  }

  screenToCoordinate(x: number, y: number, z = 0): Coordinate {
    const adjustedY = -y - z * TILE_STEP_Z;
    const isoX = (x / TILE_STEP_X + adjustedY / TILE_STEP_Y) / 2;
    const isoY = (x / TILE_STEP_X - adjustedY / TILE_STEP_Y) / 2;

    return {
      x: Math.round(isoX),
      y: Math.round(isoY),
      z,
    };
  }
}

function requireDirectionName(direction: ClientGridDirectionDefinition, gridIdentifier: string): string {
  if (typeof direction.name !== 'string' || !direction.name.trim()) {
    throw new Error(`Grid ${gridIdentifier} contains a direction without a valid name.`);
  }

  return direction.name;
}

function requireInteger(value: unknown, gridIdentifier: string, directionName: string, field: string): number {
  if (typeof value !== 'number' || !Number.isInteger(value)) {
    throw new Error(`Grid ${gridIdentifier} direction ${directionName} has invalid ${field}.`);
  }

  return value;
}

function requireFiniteNumber(value: unknown, gridIdentifier: string, directionName: string, field: string): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`Grid ${gridIdentifier} direction ${directionName} has invalid ${field}.`);
  }

  return value;
}
