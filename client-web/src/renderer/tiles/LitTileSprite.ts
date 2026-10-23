import { BatchableSprite, DefaultBatcher, ExtensionType, extensions, Sprite, SpritePipe } from 'pixi.js';
import type { BatcherOptions, DefaultBatchableMeshElement, DefaultBatchableQuadElement, InstructionSet, Renderer } from 'pixi.js';
import { TILE_STEP_X, TILE_STEP_Y } from '@/core/RenderGrid';
import { projectCoordinate } from '../IsoProjection';
import type { Coordinate } from '@/networking/GameProtocol';
import { buildSharedLightMesh } from './SharedLightMesh';
import type { LightingEnvironment, ResolvedLightColor } from '../LightingEnvironment';

const sharedSamples = new WeakMap<LightingEnvironment, { revision: number; colors: Map<string, ResolvedLightColor> }>();

interface LightMesh {
  positions: Float32Array;
  uvs: Float32Array;
  colors: Uint32Array;
  indices: Uint32Array;
  signature: number[];
  samples: { key: string; coordinate: Coordinate }[];
  sampleColors: Float32Array;
  sampleIndices: Uint32Array;
  weights: Float32Array;
}

class LitBatchElement {
  batcherName = 'litTiles';
  topology = 'triangle-list' as const;
  packAsQuad = true;
  attributeSize = 4;
  indexSize = 6;
  roundPixels: 0 | 1 = 0;
  _attributeStart = 0;
  _indexStart = 0;
  _textureId = 0;
  _batcher: BatchableSprite['_batcher'] = null!;
  _batch: BatchableSprite['_batch'] = null!;
  renderable: Sprite = null!;
  transform: BatchableSprite['transform'] = null!;
  texture: BatchableSprite['texture'] = null!;
  bounds: BatchableSprite['bounds'] = null!;
  get color(): number { return this.renderable.groupColorAlpha; }
  get blendMode() { return this.renderable.groupBlendMode; }
  destroy(): void { this.renderable = null!; this._batcher = null!; this._batch = null!; }

  positions: Float32Array = new Float32Array();
  uvs: Float32Array = new Float32Array();
  colors: Uint32Array = new Uint32Array();
  indices: Uint32Array = new Uint32Array();
  attributeOffset = 0;
  indexOffset = 0;

  refresh(): boolean {
    const sprite = this.renderable;
    const mesh = sprite instanceof LitTileSprite ? sprite.getLightMesh(sprite.visualBounds) : null;
    const size = mesh ? mesh.colors.length : 4;
    const changed = this.attributeSize !== size || this.indexSize !== (mesh ? mesh.indices.length : 6) || this.packAsQuad !== !mesh;
    this.packAsQuad = !mesh;
    this.attributeSize = size;
    this.indexSize = mesh ? mesh.indices.length : 6;
    if (mesh) {
      this.positions = mesh.positions;
      this.uvs = mesh.uvs;
      this.colors = mesh.colors;
      this.indices = mesh.indices;
    }
    return changed;
  }
}

/** Tile lighting stays in the ordinary sprite batch, including when enabled. */
export class LitTileSprite extends Sprite {
  private light: { environment: LightingEnvironment; coordinate: Coordinate; surfaceOffset: number } | null = null;
  private mesh: LightMesh | null = null;
  private colorsDirty = true;
  get smoothLightingEnabled(): boolean { return this.light !== null; }

  setSmoothLighting(environment: LightingEnvironment | null, coordinate: Coordinate, surfaceOffset: number): void {
    if (!environment && !this.light) return;
    this.light = environment ? { environment, coordinate, surfaceOffset } : null;
    this.colorsDirty = true;
    this.onViewUpdate();
  }

  getLightMesh(bounds: DefaultBatchableQuadElement['bounds']): LightMesh | null {
    if (!this.light) return null;
    const { environment, coordinate, surfaceOffset } = this.light;
    const uv = this.texture.uvs;
    const signature = [bounds.minX, bounds.minY, bounds.maxX, bounds.maxY,
      this.x, this.y, this.scale.x, this.scale.y, surfaceOffset,
      coordinate.x, coordinate.y, coordinate.z, TILE_STEP_X, TILE_STEP_Y,
      uv.x0, uv.y0, uv.x1, uv.y1, uv.x3, uv.y3];
    let geometryChanged = !this.mesh;
    if (this.mesh) {
      for (let i = 0; i < signature.length; i += 1) {
        if (this.mesh.signature[i] !== signature[i]) { geometryChanged = true; break; }
      }
    }
    if (geometryChanged) {
      this.mesh = this.buildLightMesh(bounds, signature);
      this.colorsDirty = true;
    }
    if (!this.colorsDirty) return this.mesh;
    const revision = environment.getRevision();
    let shared = sharedSamples.get(environment);
    if (!shared || shared.revision !== revision || shared.colors.size > 65536) {
      shared = { revision, colors: new Map() };
      sharedSamples.set(environment, shared);
    }
    const mesh = this.mesh!;
    for (let i = 0; i < mesh.samples.length; i += 1) {
      const sample = mesh.samples[i];
      let color = shared.colors.get(sample.key);
      if (!color) {
        color = environment.getColor(sample.coordinate);
        shared.colors.set(sample.key, color);
      }
      mesh.sampleColors[i * 3] = color.red;
      mesh.sampleColors[i * 3 + 1] = color.green;
      mesh.sampleColors[i * 3 + 2] = color.blue;
    }
    const { sampleColors, sampleIndices, weights, colors } = mesh;
    for (let i = 0; i < colors.length; i += 1) {
      const j = i * 3;
      const a = sampleIndices[j] * 3, b = sampleIndices[j + 1] * 3, c = sampleIndices[j + 2] * 3;
      const wa = weights[j], wb = weights[j + 1], wc = weights[j + 2];
      colors[i] = Math.round((sampleColors[a] * wa + sampleColors[b] * wb + sampleColors[c] * wc) * 255)
        | (Math.round((sampleColors[a + 1] * wa + sampleColors[b + 1] * wb + sampleColors[c + 1] * wc) * 255) << 8)
        | (Math.round((sampleColors[a + 2] * wa + sampleColors[b + 2] * wb + sampleColors[c + 2] * wc) * 255) << 16);
    }
    this.colorsDirty = false;
    return mesh;
  }

  /** Geometry and interpolation weights depend on artwork, never on moving lights. */
  private buildLightMesh(bounds: DefaultBatchableQuadElement['bounds'], signature: number[]): LightMesh {
    const { coordinate, surfaceOffset } = this.light!;
    const origin = projectCoordinate(coordinate);
    const left = this.x + bounds.minX * this.scale.x;
    const right = this.x + bounds.maxX * this.scale.x;
    const top = this.y + bounds.minY * this.scale.y + surfaceOffset;
    const bottom = this.y + bounds.maxY * this.scale.y + surfaceOffset;
    const triangles = buildSharedLightMesh({
      minX: Math.min(left, right), maxX: Math.max(left, right),
      minY: Math.min(top, bottom), maxY: Math.max(top, bottom),
    }, TILE_STEP_X, TILE_STEP_Y, () => ({ red: 1, green: 1, blue: 1 }));
    // Share vertices between triangles instead of uploading and recoloring duplicates.
    const vertices: typeof triangles = [];
    const vertexLookup = new Map<string, number>();
    const triangleIndices: number[] = [];
    for (const vertex of triangles) {
      const key = `${vertex.x}:${vertex.y}`;
      let index = vertexLookup.get(key);
      if (index === undefined) {
        index = vertices.length;
        vertexLookup.set(key, index);
        vertices.push(vertex);
      }
      triangleIndices.push(index);
    }
    const positions = new Float32Array(vertices.length * 2);
    const uvs = new Float32Array(vertices.length * 2);
    const colors = new Uint32Array(vertices.length);
    const indices = new Uint32Array(triangleIndices);
    const sampleIndices = new Uint32Array(vertices.length * 3);
    const weights = new Float32Array(vertices.length * 3);
    const samples: LightMesh['samples'] = [];
    const sampleLookup = new Map<string, number>();
    const addSample = (x: number, y: number): number => {
      const key = `${x}:${y}:${coordinate.z}:${TILE_STEP_X}:${TILE_STEP_Y}`;
      let index = sampleLookup.get(key);
      if (index === undefined) {
        index = samples.length;
        const dx = (x - origin.x) / (2 * TILE_STEP_X);
        const dy = (y - origin.y + TILE_STEP_Y) / (2 * TILE_STEP_Y);
        samples.push({ key, coordinate: { x: coordinate.x + dx - dy, y: coordinate.y + dx + dy, z: coordinate.z } });
        sampleLookup.set(key, index);
      }
      return index;
    };
    const uv = this.texture.uvs;
    vertices.forEach((v, i) => {
      const tx = (v.x - left) / (right - left);
      const ty = (v.y - top) / (bottom - top);
      positions[i * 2] = (v.x - this.x) / this.scale.x;
      positions[i * 2 + 1] = (v.y - this.y - surfaceOffset) / this.scale.y;
      uvs[i * 2] = uv.x0 + (uv.x1 - uv.x0) * tx + (uv.x3 - uv.x0) * ty;
      uvs[i * 2 + 1] = uv.y0 + (uv.y1 - uv.y0) * tx + (uv.y3 - uv.y0) * ty;
      const x = Math.floor(v.x / TILE_STEP_X) * TILE_STEP_X;
      const y = Math.floor(v.y / TILE_STEP_Y) * TILE_STEP_Y;
      const fx = (v.x - x) / TILE_STEP_X, fy = (v.y - y) / TILE_STEP_Y;
      const firstTriangle = fy <= fx;
      sampleIndices[i * 3] = addSample(x, y);
      sampleIndices[i * 3 + 1] = firstTriangle ? addSample(x + TILE_STEP_X, y) : addSample(x, y + TILE_STEP_Y);
      sampleIndices[i * 3 + 2] = addSample(x + TILE_STEP_X, y + TILE_STEP_Y);
      weights[i * 3] = 1 - Math.max(fx, fy);
      weights[i * 3 + 1] = Math.abs(fx - fy);
      weights[i * 3 + 2] = Math.min(fx, fy);
    });
    return { positions, uvs, colors, indices, signature, samples,
      sampleColors: new Float32Array(samples.length * 3), sampleIndices, weights };
  }
}

/** Preserve Pixi's texture batching, shaders, blend modes and index buffers. */
export class LitTileBatcher extends DefaultBatcher {
  constructor(options: BatcherOptions) {
    super(options);
    Object.defineProperty(this, 'name', { value: 'litTiles' });
  }

  override packAttributes(element: DefaultBatchableMeshElement, floats: Float32Array, uints: Uint32Array, index: number, textureId: number): void {
    super.packAttributes(element, floats, uints, index, textureId);
    if (!(element instanceof LitBatchElement)) return;
    const base = element.color;
    const red = base & 255;
    const green = (base >>> 8) & 255;
    const blue = (base >>> 16) & 255;
    const alpha = base & 0xff000000;
    for (let vertex = 0; vertex < element.attributeSize; vertex += 1) {
      const light = element.colors[vertex];
      uints[index + vertex * 6 + 4] = alpha
        | Math.round((light & 255) * red / 255)
        | (Math.round(((light >>> 8) & 255) * green / 255) << 8)
        | (Math.round(((light >>> 16) & 255) * blue / 255) << 16);
    }
  }
}

/** Route every sprite to the same batcher so entities do not break tile batches. */
export class LitSpritePipe extends SpritePipe {
  constructor(private readonly renderer: Renderer) { super(renderer); }

  override addRenderable(sprite: Sprite, instructions: InstructionSet): void {
    let element = sprite._gpuData[this.renderer.uid];
    if (!element) {
      element = new LitBatchElement() as unknown as BatchableSprite;
      element.renderable = sprite;
      element.transform = sprite.groupTransform;
      element.texture = sprite.texture;
      element.bounds = sprite.visualBounds;
      element.roundPixels = this.renderer.roundPixels || sprite.roundPixels ? 1 : 0;
      sprite._gpuData[this.renderer.uid] = element;
    }
    (element as unknown as LitBatchElement).refresh();
    element.batcherName = 'litTiles';
    super.addRenderable(sprite, instructions);
  }

  override validateRenderable(sprite: Sprite): boolean {
    const element = sprite._gpuData[this.renderer.uid] as unknown as LitBatchElement | undefined;
    if (!element || !element._batcher) return true;
    const changed = element.refresh();
    return super.validateRenderable(sprite) || changed;
  }

  override updateRenderable(sprite: Sprite): void {
    (sprite._gpuData[this.renderer.uid] as unknown as LitBatchElement).refresh();
    super.updateRenderable(sprite);
  }
}

extensions.add({ type: ExtensionType.Batcher, name: 'litTiles', ref: LitTileBatcher });
extensions.remove(SpritePipe);
extensions.add(LitSpritePipe);
