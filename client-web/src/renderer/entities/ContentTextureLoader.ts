import { Texture } from 'pixi.js';
import type { ClientAssetManifest } from '@/core/services/ClientAssetManifest';
import { resolveServerUrl } from '@/data/ClientRegistryLoader';
import { getModAsset } from '@/core/services/ModAssetStore';

export class ContentTextureLoader {
  private readonly texturePromises = new Map<string, Promise<Texture>>();

  constructor(
    private readonly assetManifest: ClientAssetManifest,
    private readonly serverApiUrl: string,
    private readonly authToken: string,
  ) {}

  load(texturePath: string): Promise<Texture> {
    const existing = this.texturePromises.get(texturePath);
    if (existing) {
      return existing;
    }

    const promise = this.fetchTexture(texturePath);
    this.texturePromises.set(texturePath, promise);
    return promise;
  }

  private async fetchTexture(texturePath: string): Promise<Texture> {
    const modAsset = await getModAsset(texturePath);
    let blob: Blob;
    if (modAsset) {
      blob = modAsset;
    } else {
      const response = await fetch(this.resolveTextureUrl(texturePath), {
        headers: { Authorization: `Bearer ${this.authToken}` },
      });
      if (!response.ok) throw new Error(`Failed to fetch texture: ${response.status} ${response.statusText}`);
      blob = await response.blob();
    }

    const imageBitmap = await createImageBitmap(blob);
    const texture = Texture.from({
      resource: imageBitmap,
      scaleMode: 'nearest',
      autoGenerateMipmaps: false,
      antialias: false,
    }, true);
    return texture;
  }

  private resolveTextureUrl(texturePath: string): string {
    const normalizedTexturePath = texturePath.replace(/^\/+/, '');
    const manifestUrl = this.assetManifest.assets[normalizedTexturePath];

    if (manifestUrl) {
      return resolveServerUrl(this.serverApiUrl, manifestUrl);
    }

    return resolveServerUrl(this.serverApiUrl, `/client/content/${normalizedTexturePath}`);
  }
}
