import type { GridApi } from '@/api/GridApi';
import { LuaArguments } from './LuaArguments';
import { toLuaCoordinate, toLuaDirection } from './LuaObjects';
import type { LuaRuntime } from './LuaRuntime';

export async function registerGridLuaModule(runtime: LuaRuntime, grid: GridApi): Promise<void> {
  const defineDirection = (name: unknown, x: unknown, y: unknown, z: unknown, angle: unknown) => {
    const args = new LuaArguments('selene.grid.defineDirection');

    return toLuaCoordinate(grid.defineDirection(
      args.string(name, 'name'),
      args.integer(x, 'x'),
      args.integer(y, 'y'),
      args.integer(z, 'z'),
      args.finiteNumber(angle, 'angle'),
    ));
  };
  const getDirectionByName = (name: unknown) => {
    const args = new LuaArguments('selene.grid.getDirectionByName');

    return toLuaDirection(grid.getDirectionByName(args.string(name, 'name')));
  };
  const screenToCoordinate = (x: unknown, y: unknown, z?: unknown) => {
    const args = new LuaArguments('selene.grid.screenToCoordinate');

    return toLuaCoordinate(grid.screenToCoordinate(
      args.finiteNumber(x, 'x'),
      args.finiteNumber(y, 'y'),
      z === undefined ? undefined : args.integer(z, 'z'),
    ));
  };

  await runtime.preloadModule('selene.grid', () => ({
    defineDirection,
    getDirectionByName,
    screenToCoordinate,
  }));
}
