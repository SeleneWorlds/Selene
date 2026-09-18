import type { ClientDirection } from '@/api/GridApi';
import type { MovementGridApi } from '@/api/MovementGridApi';
import { LuaArguments } from './LuaArguments';
import type { LuaRuntime } from './LuaRuntime';

export async function registerMovementGridLuaModule(runtime: LuaRuntime, movementGrid: MovementGridApi): Promise<void> {
  const setMotion = (directionValue: unknown) => {
    movementGrid.setMotion(parseDirection(directionValue, 'selene.movement.grid.setMotion'));
  };
  const setFacing = (directionValue: unknown) => {
    movementGrid.setFacing(parseDirection(directionValue, 'selene.movement.grid.setFacing'));
  };

  await runtime.preloadModule('selene.movement.grid', () => ({
    setMotion,
    setFacing,
  }));
}

function parseDirection(value: unknown, context: string): ClientDirection {
  if (typeof value !== 'object' || value === null) {
    throw new Error(`${context} expected direction to be a direction.`);
  }

  const args = new LuaArguments(context);
  const direction = value as Partial<ClientDirection> & {
    getName?: () => unknown;
    getVector?: () => unknown;
    getAngle?: () => unknown;
  };

  return {
    name: args.string(direction.name ?? direction.getName?.(), 'direction.name'),
    vector: args.coordinate(direction.vector ?? direction.getVector?.(), 'direction.vector'),
    angle: args.finiteNumber(direction.angle ?? direction.getAngle?.(), 'direction.angle'),
  };
}
