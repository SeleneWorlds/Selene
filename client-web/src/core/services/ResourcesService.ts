import type { ResourcesApi } from '@/api/ResourcesApi';
import { resolveServerUrl } from '@/data/ClientRegistryLoader';
import type { ClientAssetManifest } from './ClientAssetManifest';

export class ResourcesService implements ResourcesApi {
  constructor(private readonly manifest: ClientAssetManifest, private readonly serverUrl: string, private readonly token: string) {}
  listFiles(bundle: string, filter: string) { const pattern = globRegex(filter); return Object.keys(this.manifest.assets).filter((p) => pattern.test(p)).map((p) => `${bundle}/${p}`); }
  fileExists(path: string) { return this.logical(path) in this.manifest.assets; }
  async loadAsString(pathValue: string) {
    const path = this.logical(pathValue);
    const asset = this.manifest.assets[path];
    if (!asset) throw new Error(`File not found: ${pathValue}`);

    const response = await fetch(resolveServerUrl(this.serverUrl, asset), {
      headers: { Authorization: `Bearer ${this.token}` },
    });
    if (!response.ok) throw new Error(`Failed to load resource: ${response.status}`);
    return response.text();
  }
  private logical(path: string) { const normalized = path.replace(/^\/+/, ''); return normalized.includes('/') ? normalized.slice(normalized.indexOf('/') + 1) : normalized; }
}

function globRegex(glob: string): RegExp { return new RegExp(`^${glob.replace(/[.+^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*').replace(/\?/g, '.')}$`); }
