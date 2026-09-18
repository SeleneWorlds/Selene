import { getSortLayer, ROW_SORT_SCALE } from '@/renderer/IsoProjection';
import type { Coordinate } from '@/networking/GameProtocol';
import type { WorldBounds } from '../entities/PixiEntityLayer';
import type { RenderedTileStack } from './RenderedTileStack';
import type { TileSpatialIndex } from './TileSpatialIndex';

const OCCLUDED_ALPHA = 0.3;
const FADE_SPEED = 5;

export class TileOcclusionFader {
  private readonly fadingStacks = new Set<RenderedTileStack>();
  private lastFocus: Coordinate | null = null;
  private lastBounds: WorldBounds | null = null;
  private indexRevision = -1;
  private fading = false;

  constructor(private readonly spatialIndex: TileSpatialIndex) {}

  remove(stack: RenderedTileStack): void {
    this.fadingStacks.delete(stack);
    this.indexRevision = -1;
  }

  update(deltaMs: number, focus: Coordinate, focusBounds: WorldBounds): void {
    if (this.indexRevision === this.spatialIndex.revision && !this.fading
      && sameCoordinate(this.lastFocus, focus) && sameBounds(this.lastBounds, focusBounds)) return;
    this.indexRevision = this.spatialIndex.revision;
    this.fading = false;
    this.lastFocus = { ...focus };
    this.lastBounds = { ...focusBounds };
    const candidates = this.spatialIndex.query(focusBounds);
    for (const stack of this.fadingStacks) candidates.add(stack);
    for (const stack of candidates) {
      this.fading = fadeStack(stack, focus, focusBounds, deltaMs) || this.fading;
      if (stack.occlusionAlphas.some(alpha => alpha !== 1)) this.fadingStacks.add(stack);
      else this.fadingStacks.delete(stack);
    }
  }

  setUpperLayerAlpha(focusZ: number, alpha: number, stacks: Iterable<RenderedTileStack>): void {
    for (const stack of stacks) {
      stack.upperLayerAlpha = stack.coordinate.z > focusZ ? alpha : 1;
      for (const [index, container] of stack.containers.entries()) {
        container.alpha = stack.upperLayerAlpha * stack.occlusionAlphas[index];
      }
    }
  }
}

function fadeStack(stack: RenderedTileStack, focus: Coordinate, focusBounds: WorldBounds, deltaMs: number): boolean {
  const inFront = getSortLayer(focus) - getSortLayer(stack.coordinate) >= ROW_SORT_SCALE;
  for (let index = 0; index < stack.containers.length; index += 1) {
    const container = stack.containers[index];
    const bounds = stack.localBounds[index];
    const current = stack.occlusionAlphas[index];
    if (!inFront && current === 1) continue;
    const overlapsFocus = bounds !== null && overlapsTranslated(bounds, container.x, container.y, focusBounds);
    const canOcclude = bounds !== null && (bounds.height >= focusBounds.height || stack.coordinate.z > focus.z);
    stack.targetOcclusionAlphas[index] = inFront && overlapsFocus && canOcclude ? OCCLUDED_ALPHA : 1;
    const target = stack.targetOcclusionAlphas[index];
    const step = FADE_SPEED * (deltaMs / 1000);
    const next = Math.abs(target - current) <= step ? target : current + Math.sign(target - current) * step;
    if (next !== current) {
      stack.occlusionAlphas[index] = next;
      container.alpha = stack.upperLayerAlpha * next;
    }
  }
  return stack.occlusionAlphas.some((alpha, index) => alpha !== stack.targetOcclusionAlphas[index]);
}

function overlapsTranslated(left: WorldBounds, x: number, y: number, right: WorldBounds): boolean {
  return left.x + x < right.x + right.width && left.x + x + left.width > right.x
    && left.y + y < right.y + right.height && left.y + y + left.height > right.y;
}

function sameCoordinate(left: Coordinate | null, right: Coordinate): boolean {
  return left !== null && left.x === right.x && left.y === right.y && left.z === right.z;
}

function sameBounds(left: WorldBounds | null, right: WorldBounds): boolean {
  return left !== null && left.x === right.x && left.y === right.y
    && left.width === right.width && left.height === right.height;
}
