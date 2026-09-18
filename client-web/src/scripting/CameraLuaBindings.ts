import type { CameraApi } from '@/api/CameraApi';
import { LuaMultiReturn } from 'wasmoon';
import type { LuaRuntime } from './LuaRuntime';
import { LuaArguments } from './LuaArguments';
import { toLuaCoordinate } from './LuaObjects';
import { createSignal } from './LuaSignal';

export async function registerCameraLuaModule(runtime: LuaRuntime, camera: CameraApi): Promise<void> {
  const getCoordinate = () => toLuaCoordinate(camera.getCoordinate());
  const setViewport = (x: unknown, y: unknown, width: unknown, height: unknown) => {
    const args = new LuaArguments('selene.camera.setViewport');

    camera.setViewport(
      args.integer(x, 'x'),
      args.integer(y, 'y'),
      args.integer(width, 'width'),
      args.integer(height, 'height'),
    );
  };
  const screenToWorld = (screenX: unknown, screenY: unknown) => {
    const args = new LuaArguments('selene.camera.screenToWorld');
    const world = camera.screenToWorld(
      args.finiteNumber(screenX, 'screenX'),
      args.finiteNumber(screenY, 'screenY'),
    );

    return LuaMultiReturn.of(world.x, world.y);
  };
  const onCoordinateChanged = createSignal('selene.camera.onCoordinateChanged', (callback) => {
    return camera.addCoordinateChangedListener((coordinate) => {
      callback(toLuaCoordinate(coordinate));
    });
  });

  await runtime.preloadModule('selene.camera', () => ({
    getCoordinate,
    setViewport,
    screenToWorld,
    onCoordinateChanged,
  }));
}
