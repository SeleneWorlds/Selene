import { Container } from 'pixi.js';
import { projectCoordinate } from '@/renderer/IsoProjection';
import type { ClientCamera } from '@/core/ClientCamera';
import type { Coordinate } from '@/networking/GameProtocol';
import type { PixiEntityLayer, WorldBounds } from '@/renderer/entities/PixiEntityLayer';

export interface SceneFocus {
  coordinate: Coordinate;
  bounds: WorldBounds;
}

export class PixiScene {
  readonly container = new Container();
  readonly depthSortedContainer = new Container();

  constructor(private readonly camera: ClientCamera) {
    this.container.enableRenderGroup();
    this.depthSortedContainer.sortableChildren = true;
    this.container.addChild(this.depthSortedContainer);
  }

  updateCamera(entityLayer: PixiEntityLayer): void {
    this.camera.updateFollowedCoordinate((id) => entityLayer.getEntityCoordinate(id));
    const position = this.camera.getWorldPosition();
    this.container.position.set(Math.round(position.x), Math.round(position.y));
  }

  getFocus(entityLayer: PixiEntityLayer): SceneFocus {
    const coordinate = this.camera.getCoordinate();
    const followedId = this.camera.getFollowedEntityId();
    const entityBounds = followedId === null ? null : entityLayer.getEntityBounds(followedId);
    const projected = projectCoordinate(coordinate);
    return {
      coordinate,
      bounds: entityBounds ?? { x: projected.x, y: projected.y, width: 1, height: 1 },
    };
  }

  getViewBounds(width: number, height: number): WorldBounds {
    return { x: -this.container.x, y: -this.container.y, width, height };
  }
}
