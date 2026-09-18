import type { Coordinate } from '@/networking/GameProtocol';

export type ClientCameraCoordinateChangedListener = (coordinate: Coordinate) => void;

export interface CameraApi {
  getCoordinate: () => Coordinate;
  setViewport: (x: number, y: number, width: number, height: number) => void;
  screenToWorld: (screenX: number, screenY: number) => { x: number; y: number };
  addCoordinateChangedListener: (listener: ClientCameraCoordinateChangedListener) => () => void;
}
