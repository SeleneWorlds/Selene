export interface ClientMapTile {
  x: number;
  y: number;
  z: number;
  tileIds: readonly number[];
  visualMetadata: Readonly<Record<string, unknown>>;
}
