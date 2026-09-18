import { Rectangle } from 'pixi.js';
import type { WorldBounds } from '../entities/PixiEntityLayer';
import type { RenderedTileStack } from './RenderedTileStack';

const BUCKET_SIZE = 256;

export class TileSpatialIndex {
  private readonly buckets = new Map<string, Set<RenderedTileStack>>();
  revision = 0;

  query(bounds: WorldBounds): Set<RenderedTileStack> {
    const result = new Set<RenderedTileStack>();
    forEachBucketKey(bounds, key => {
      const bucket = this.buckets.get(key);
      if (bucket) for (const stack of bucket) result.add(stack);
    });
    return result;
  }

  refreshBounds(stack: RenderedTileStack, index: number): void {
    if (!cacheContainerBounds(stack, index)) return;
    this.remove(stack);
    let x = Infinity;
    let y = Infinity;
    let right = -Infinity;
    let bottom = -Infinity;
    for (let i = 0; i < stack.localBounds.length; i += 1) {
      const bounds = stack.localBounds[i];
      if (!bounds) continue;
      const container = stack.containers[i];
      x = Math.min(x, bounds.x + container.x);
      y = Math.min(y, bounds.y + container.y);
      right = Math.max(right, bounds.x + container.x + bounds.width);
      bottom = Math.max(bottom, bounds.y + container.y + bounds.height);
    }
    if (x === Infinity) {
      stack.cullingBounds = null;
      return;
    }
    stack.cullingBounds = { x, y, width: right - x, height: bottom - y };
    forEachBucketKey(stack.cullingBounds, key => {
      let bucket = this.buckets.get(key);
      if (!bucket) {
        bucket = new Set();
        this.buckets.set(key, bucket);
      }
      bucket.add(stack);
      stack.cullingBucketKeys.push(key);
    });
  }

  remove(stack: RenderedTileStack): void {
    for (const key of stack.cullingBucketKeys) {
      const bucket = this.buckets.get(key);
      bucket?.delete(stack);
      if (bucket?.size === 0) this.buckets.delete(key);
    }
    stack.cullingBucketKeys.length = 0;
    this.revision += 1;
  }
}

function forEachBucketKey(bounds: WorldBounds, visit: (key: string) => void): void {
  const minX = Math.floor(bounds.x / BUCKET_SIZE);
  const maxX = Math.floor((bounds.x + bounds.width) / BUCKET_SIZE);
  const minY = Math.floor(bounds.y / BUCKET_SIZE);
  const maxY = Math.floor((bounds.y + bounds.height) / BUCKET_SIZE);
  for (let y = minY; y <= maxY; y += 1) {
    for (let x = minX; x <= maxX; x += 1) visit(`${x}:${y}`);
  }
}

function cacheContainerBounds(stack: RenderedTileStack, index: number): boolean {
  const container = stack.containers[index];
  const bounds = container.getLocalBounds();
  const previous = stack.localBounds[index];
  if (previous !== null && previous.x === bounds.x && previous.y === bounds.y
    && previous.width === bounds.width && previous.height === bounds.height) return false;
  stack.localBounds[index] = { x: bounds.x, y: bounds.y, width: bounds.width, height: bounds.height };
  if (container.cullArea) Object.assign(container.cullArea, stack.localBounds[index]);
  else container.cullArea = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
  return true;
}
