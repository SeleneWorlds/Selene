import { Container, Graphics, Sprite } from 'pixi.js';
import { getRenderOrder, projectCoordinate, TILE_HEIGHT, TILE_WIDTH } from '@/renderer/IsoProjection';
import type { Coordinate } from '@/networking/GameProtocol';
import type { ContentTextureLoader } from '../entities/ContentTextureLoader';
import type { RenderedAnimatedTile, RenderedTileStack } from './RenderedTileStack';
import type { TileVisualResolver } from './TileVisualResolver';

export class PixiTileStackRenderer {
  private readonly animatedStacks = new Set<RenderedTileStack>();
  private elapsedMs = 0;

  constructor(
    private readonly visualResolver: TileVisualResolver,
    private readonly textureLoader: ContentTextureLoader,
    private readonly parent: Container,
    private readonly boundsChanged: (stack: RenderedTileStack, index: number) => void,
  ) {}

  createStack(coordinate: Coordinate, tileIds: readonly number[], generation: number): RenderedTileStack {
    const position = projectCoordinate(coordinate);
    const containers = tileIds.map((tileId, localSortLayer) => {
      const visual = this.visualResolver.resolve(tileId, coordinate);
      // A Sprite is itself a Container in Pixi v8. Avoiding a wrapper around
      // every ordinary tile roughly halves scene-graph traversal for maps.
      const container = visual ? new Sprite() : new Container();
      container.position.set(position.x, position.y);
      container.zIndex = getRenderOrder(coordinate, visual?.sortLayerOffset ?? 0, localSortLayer);
      this.parent.addChild(container);
      return container;
    });

    return {
      containers,
      localBounds: containers.map(() => null),
      coordinate,
      tileIds,
      generation,
      animatedTiles: [],
      occlusionAlphas: containers.map(() => 1),
      targetOcclusionAlphas: containers.map(() => 1),
      upperLayerAlpha: 1,
      cullingBounds: null,
      cullingBucketKeys: [],
    };
  }

  removeStack(stack: RenderedTileStack): void {
    this.animatedStacks.delete(stack);
    for (const container of stack.containers) container.destroy({ children: true });
  }

  updateAnimations(deltaMs: number, isCurrent: (stack: RenderedTileStack) => boolean): void {
    this.elapsedMs += deltaMs;
    for (const stack of this.animatedStacks) {
      for (const tile of stack.animatedTiles) {
        const animation = tile.visual.animation!;
        if (animation.instanced) tile.elapsedMs += deltaMs;
        const elapsedMs = animation.instanced ? tile.elapsedMs : this.elapsedMs;
        const frameDurationMs = (animation.duration * 1000) / animation.textures.length;
        const nextFrame = Math.floor(elapsedMs / frameDurationMs) % animation.textures.length;
        if (nextFrame !== tile.frameIndex) {
          tile.frameIndex = nextFrame;
          void this.applyAnimationFrame(stack, tile, isCurrent);
        }
      }
    }
  }

  async renderStack(stack: RenderedTileStack, isCurrent: (stack: RenderedTileStack) => boolean): Promise<void> {
    const generation = stack.generation;
    for (const [localSortLayer, tileId] of stack.tileIds.entries()) {
      const container = stack.containers[localSortLayer];
      const visual = this.visualResolver.resolve(tileId, stack.coordinate);
      if (!visual) {
        container.addChild(createFallbackTile(tileId));
        this.boundsChanged(stack, localSortLayer);
        continue;
      }

      try {
        const texture = await this.textureLoader.load(visual.texturePath);
        if (stack.generation !== generation || !isCurrent(stack)) return;
        const sprite = container as Sprite;
        sprite.texture = texture;
        sprite.anchor.set(0.5, 1);
        const position = projectCoordinate(stack.coordinate);
        sprite.position.set(position.x + visual.offsetX, position.y - visual.offsetY);
        sprite.scale.x = visual.flipX ? -1 : 1;
        sprite.scale.y = visual.flipY ? -1 : 1;
        this.boundsChanged(stack, localSortLayer);
        if (visual.animation && visual.animation.textures.length > 1) {
          stack.animatedTiles.push({ sprite, visual, elapsedMs: 0, frameIndex: 0, revision: 0 });
          this.animatedStacks.add(stack);
        }
      } catch (error) {
        console.warn(`Failed to load tile texture ${visual.texturePath}.`, error);
        if (stack.generation === generation && isCurrent(stack)) {
          container.addChild(createFallbackTile(tileId));
          this.boundsChanged(stack, localSortLayer);
        }
      }
    }
  }

  private async applyAnimationFrame(
    stack: RenderedTileStack,
    tile: RenderedAnimatedTile,
    isCurrent: (stack: RenderedTileStack) => boolean,
  ): Promise<void> {
    const texturePath = tile.visual.animation!.textures[tile.frameIndex];
    const revision = ++tile.revision;
    try {
      const texture = await this.textureLoader.load(texturePath);
      if (revision === tile.revision && isCurrent(stack)) {
        tile.sprite.texture = texture;
        const index = stack.containers.indexOf(tile.sprite);
        if (index >= 0) this.boundsChanged(stack, index);
      }
    } catch (error) {
      console.warn(`Failed to load tile animation texture ${texturePath}.`, error);
    }
  }
}

function createFallbackTile(tileId: number): Graphics {
  const hue = (Math.abs(tileId) * 2654435761) >>> 0;
  const color = 0x335f3f + (hue & 0x303030);
  return new Graphics()
    .moveTo(0, -TILE_HEIGHT / 2)
    .lineTo(TILE_WIDTH / 2, 0)
    .lineTo(0, TILE_HEIGHT / 2)
    .lineTo(-TILE_WIDTH / 2, 0)
    .closePath()
    .fill({ color, alpha: 0.7 })
    .stroke({ color: 0xdce8d8, alpha: 0.35, width: 1 });
}
