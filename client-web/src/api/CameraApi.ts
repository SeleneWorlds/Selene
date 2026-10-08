import type { Coordinate } from '@/networking/GameProtocol';

export type ClientCameraCoordinateChangedListener = (coordinate: Coordinate) => void;

export interface CameraApi {
  setZoom: (zoom: number) => number;
  getCoordinate: () => Coordinate;
  getPosition: () => { x: number; y: number };
  setPosition: (position: { x: number; y: number }) => Coordinate;
  setViewport: (x: number, y: number, width: number, height: number) => void;
  screenToWorld: (screenX: number, screenY: number) => { x: number; y: number };
  addCoordinateChangedListener: (listener: ClientCameraCoordinateChangedListener) => () => void;
}
