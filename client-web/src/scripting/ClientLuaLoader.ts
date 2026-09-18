import { resolveServerUrl } from '@/data/ClientRegistryLoader';
import {
  ClientLuaIndexResponseSchema,
  ClientLuaModuleSourceResponseSchema,
  parseServerResponse,
} from '@/data/ClientServerResponseSchemas';
import { LuaRuntime } from './LuaRuntime';

export interface ClientLuaLoadOptions {
  serverApiUrl: string;
  authToken: string;
  runtime: LuaRuntime;
}

export async function loadAndRunClientLua(options: ClientLuaLoadOptions): Promise<void> {
  const index = parseServerResponse(ClientLuaIndexResponseSchema, await fetchJson(
    resolveServerUrl(options.serverApiUrl, '/client/lua'),
    options.authToken,
    'client Lua index',
  ), 'client Lua index');

  const modules = await Promise.all(
    Object.entries(index.modules).map(async ([moduleName, moduleIndexEntry]) => {
      const moduleSource = parseServerResponse(ClientLuaModuleSourceResponseSchema, await fetchJson(
        resolveServerUrl(options.serverApiUrl, moduleIndexEntry.url),
        options.authToken,
        `${moduleName} Lua module`,
      ), `${moduleName} Lua module`);

      if (moduleSource.module !== moduleName) {
        throw new Error(`Invalid ${moduleName} Lua module: response identifies itself as ${moduleSource.module}.`);
      }
      if (moduleSource.hash !== moduleIndexEntry.hash) {
        throw new Error(`Invalid ${moduleName} Lua module: hash does not match the client Lua index.`);
      }

      return moduleSource;
    }),
  );

  await options.runtime.installModules(modules);

  for (const entrypoint of index.entrypoints) {
    await options.runtime.runEntrypoint(entrypoint.module);
  }

  console.log(`[Lua] Loaded ${modules.length} modules and ran ${index.entrypoints.length} entrypoints`);
}

async function fetchJson(url: string, authToken: string, label: string): Promise<unknown> {
  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${authToken}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch ${label}: ${response.status} ${response.statusText}`);
  }

  return response.json();
}
