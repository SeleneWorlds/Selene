import type { ScriptableTextureApi, TexturesApi } from '@/api/TexturesApi';
import { clamp, isRecord, numberOr } from './utils';

export class TexturesService implements TexturesApi { create(width: number, height: number) { return new ScriptableTexture(width, height); } }
class ScriptableTexture implements ScriptableTextureApi {
  private readonly pixels: Uint8ClampedArray; private disposed = false;
  constructor(private readonly width: number, private readonly height: number) { if (!Number.isInteger(width) || width <= 0 || !Number.isInteger(height) || height <= 0) throw new Error('Texture dimensions must be positive integers.'); this.pixels = new Uint8ClampedArray(width * height * 4); }
  getWidth() { return this.width; } getHeight() { return this.height; }
  setPixel(x: number, y: number, color: unknown) { this.ensure(); this.pixels.set(parseColor(color), this.offset(x, y)); }
  getPixel(x: number, y: number) { this.ensure(); const i = this.offset(x, y); return [this.pixels[i] / 255, this.pixels[i + 1] / 255, this.pixels[i + 2] / 255, this.pixels[i + 3] / 255] as const; }
  fill(color: unknown) { this.ensure(); const rgba = parseColor(color); for (let i = 0; i < this.pixels.length; i += 4) this.pixels.set(rgba, i); }
  copyFrom(source: ScriptableTextureApi, sx: number, sy: number, width: number, height: number, dx: number, dy: number) { this.ensure(); if (!(source instanceof ScriptableTexture)) throw new Error('Expected a scriptable texture.'); for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) this.pixels.set(source.pixels.subarray(source.offset(sx + x, sy + y), source.offset(sx + x, sy + y) + 4), this.offset(dx + x, dy + y)); }
  update() { this.ensure(); } dispose() { this.disposed = true; }
  private ensure() { if (this.disposed) throw new Error('Texture has been disposed.'); }
  private offset(x: number, y: number) { if (!Number.isInteger(x) || !Number.isInteger(y) || x < 0 || y < 0 || x >= this.width || y >= this.height) throw new Error('Pixel coordinate is outside the texture.'); return (y * this.width + x) * 4; }
}

function parseColor(value: unknown): Uint8ClampedArray {
  if (typeof value === 'string' && /^#[0-9a-f]{6}([0-9a-f]{2})?$/i.test(value)) { const hex = value.slice(1); return new Uint8ClampedArray([0, 2, 4, 6].map((i) => i < hex.length ? parseInt(hex.slice(i, i + 2), 16) : 255)); }
  if (isRecord(value)) return new Uint8ClampedArray([value.r ?? value.red, value.g ?? value.green, value.b ?? value.blue, value.a ?? value.alpha].map((v) => clamp(numberOr(v, 1), 0, 1) * 255));
  throw new Error('Expected a color table or hex color string.');
}
