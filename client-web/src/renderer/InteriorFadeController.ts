import type { Coordinate } from '@/networking/GameProtocol';

const INTERIOR_FADE_SPEED = 20;
const HIDDEN_LAYER_ALPHA = 0;

interface TilePresence {
  hasTileAt(coordinate: Coordinate): boolean;
}

interface UpperLayerTarget {
  setUpperLayerAlpha(focusZ: number, alpha: number): void;
}

export class InteriorFadeController {
  private upperLayerAlpha = 1;

  constructor(
    private readonly tiles: TilePresence & UpperLayerTarget,
    private readonly entities: UpperLayerTarget,
  ) {}

  update(deltaMs: number, focus: Coordinate): void {
    // Moving entities use interpolated coordinates, while tiles occupy integral coordinates.
    const focusTile = { x: Math.round(focus.x), y: Math.round(focus.y), z: Math.round(focus.z) };
    const isInside = focusTile.z < 0 || this.tiles.hasTileAt({ ...focusTile, z: focusTile.z + 1 });
    const targetAlpha = isInside ? HIDDEN_LAYER_ALPHA : 1;
    const blend = Math.min(1, (deltaMs / 1000) * INTERIOR_FADE_SPEED);
    this.upperLayerAlpha += (targetAlpha - this.upperLayerAlpha) * blend;
    if (Math.abs(targetAlpha - this.upperLayerAlpha) < 0.001) {
      this.upperLayerAlpha = targetAlpha;
    }
    this.tiles.setUpperLayerAlpha(focusTile.z, this.upperLayerAlpha);
    this.entities.setUpperLayerAlpha(focusTile.z, this.upperLayerAlpha);
  }
}
