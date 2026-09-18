import { resolveServerUrl } from '@/data/ClientRegistryLoader';
import {
  ClientAssetManifestSchema,
  parseServerResponse,
  type ClientAssetManifestResponse,
} from '@/data/ClientServerResponseSchemas';

export type ClientAssetManifest = ClientAssetManifestResponse;

export interface ClientAssetManifestOptions {
  serverApiUrl: string;
  authToken: string;
}

export async function loadClientAssetManifest(options: ClientAssetManifestOptions): Promise<ClientAssetManifest> {
  const response = await fetch(resolveServerUrl(options.serverApiUrl, '/client/asset-manifest.json'), {
    headers: { Authorization: `Bearer ${options.authToken}` },
  });
  if (!response.ok) throw new Error(`Failed to load client asset manifest: ${response.status}`);
  return parseServerResponse(ClientAssetManifestSchema, await response.json(), 'client asset manifest');
}
