import type { WorldBounds } from '../entities/PixiEntityLayer';
import type { RenderedTileStack } from './RenderedTileStack';
import type { TileSpatialIndex } from './TileSpatialIndex';

const CULLING_MARGIN = 128;

export class TileCuller {
  private enabled = true;
  private readonly visibleStacks = new Set<RenderedTileStack>();
  private lastView: WorldBounds | null = null;
  private indexRevision = -1;

  constructor(private readonly spatialIndex: TileSpatialIndex) {}

  add(stack: RenderedTileStack): void {
    for (const container of stack.containers) container.cullable = this.enabled;
    this.indexRevision = -1;
  }

  remove(stack: RenderedTileStack): void {
    this.visibleStacks.delete(stack);
    this.indexRevision = -1;
  }

  boundsChanged(stack: RenderedTileStack): void {
    this.indexRevision = -1;
    if (this.enabled) {
      setCulled(stack, true);
      this.visibleStacks.delete(stack);
    }
  }

  setEnabled(enabled: boolean, stacks: Iterable<RenderedTileStack>): void {
    this.enabled = enabled;
    for (const stack of stacks) {
      for (const container of stack.containers) {
        container.cullable = enabled;
        container.culled = enabled;
      }
    }
    this.visibleStacks.clear();
    this.indexRevision = -1;
  }

  update(view: WorldBounds): void {
    if (this.indexRevision === this.spatialIndex.revision && containsBounds(this.lastView, view)) return;
    this.indexRevision = this.spatialIndex.revision;
    const expanded = {
      x: view.x - CULLING_MARGIN,
      y: view.y - CULLING_MARGIN,
      width: view.width + CULLING_MARGIN * 2,
      height: view.height + CULLING_MARGIN * 2,
    };
    this.lastView = expanded;
    const candidates = this.spatialIndex.query(expanded);
    for (const stack of this.visibleStacks) if (!candidates.has(stack)) setCulled(stack, true);
    this.visibleStacks.clear();
    for (const stack of candidates) {
      const visible = stack.cullingBounds !== null && overlaps(stack.cullingBounds, expanded);
      setCulled(stack, !visible);
      if (visible) this.visibleStacks.add(stack);
    }
  }
}

function setCulled(stack: RenderedTileStack, culled: boolean): void {
  for (const container of stack.containers) container.culled = culled;
}

function overlaps(left: WorldBounds, right: WorldBounds): boolean {
  return left.x < right.x + right.width && left.x + left.width > right.x
    && left.y < right.y + right.height && left.y + left.height > right.y;
}

function containsBounds(outer: WorldBounds | null, inner: WorldBounds): boolean {
  return outer !== null && outer.x <= inner.x && outer.y <= inner.y
    && outer.x + outer.width >= inner.x + inner.width
    && outer.y + outer.height >= inner.y + inner.height;
}
