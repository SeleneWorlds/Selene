import type { ClientDirection } from '@/api/GridApi';

export interface MovementGridApi {
  setMotion: (direction: ClientDirection) => void;
  setFacing: (direction: ClientDirection) => void;
}
