import { LuaFactory } from 'wasmoon';
import luaWasmUrl from 'wasmoon/dist/glue.wasm?url';

export interface LuaModuleSource {
  module: string;
  source: string;
}

export class LuaRuntime {
  private readonly factory = new LuaFactory(luaWasmUrl);
  private readonly registeredClientEntityScripts = new Map<number, string>();
  private enginePromise: ReturnType<LuaFactory['createEngine']> | null = null;
  private engine: Awaited<ReturnType<LuaFactory['createEngine']>> | null = null;

  async installModules(modules: LuaModuleSource[]): Promise<void> {
    await Promise.all(
      modules.map((moduleSource) => this.factory.mountFile(
        getModulePath(moduleSource.module),
        moduleSource.source,
      )),
    );

    const engine = await this.getEngine();
    await engine.doString(`
package.path = "/client/lua/modules/?.lua;/client/lua/modules/?/init.lua;" .. package.path
`);
  }

  async runEntrypoint(moduleName: string): Promise<void> {
    const engine = await this.getEngine();

    console.log(`[Lua] Running entrypoint ${moduleName}`);
    await engine.doFile(getModulePath(moduleName));
  }

  async runScript(script: string): Promise<void> {
    const engine = await this.getEngine();

    await engine.doString(script);
  }

  runClientEntityScript(
    scriptModule: string,
    entityId: number,
    entity: unknown,
    deltaSeconds: number,
  ): void {
    if (!this.engine) {
      throw new Error('Lua runtime is not initialized.');
    }

    if (this.registeredClientEntityScripts.get(entityId) !== scriptModule) {
      this.engine.global.call('__selene_register_client_entity', scriptModule, entityId, entity);
      this.registeredClientEntityScripts.set(entityId, scriptModule);
    }
    this.engine.global.call('__selene_tick_client_entity', entityId, deltaSeconds);
  }

  removeClientEntityScript(entityId: number): void {
    if (!this.engine || !this.registeredClientEntityScripts.delete(entityId)) {
      return;
    }
    this.engine.global.call('__selene_remove_client_entity', entityId);
  }

  async preloadModule(moduleName: string, loader: () => unknown): Promise<void> {
    const engine = await this.getEngine();

    engine.global.getTable('package', (packageIndex) => {
      engine.global.lua.lua_getfield(engine.global.address, packageIndex, 'preload');
      engine.global.setField(-1, moduleName, loader);
      engine.global.pop(1);
    });
  }

  private getEngine(): ReturnType<LuaFactory['createEngine']> {
    this.enginePromise ??= this.factory.createEngine().then((engine) => {
      engine.global.set('print', (...values: unknown[]) => {
        console.log('[Lua]', ...values.map((value) => String(value)));
      });

      engine.doStringSync(`
local clientScriptDataByEntity = {}
local clientScriptEntityById = {}
local clientScriptTickByEntity = {}

function __selene_register_client_entity(moduleName, entityId, entity)
    local module = require(moduleName)
    clientScriptEntityById[entityId] = entity
    clientScriptTickByEntity[entityId] = module.TickEntity or false
    clientScriptDataByEntity[entityId] = clientScriptDataByEntity[entityId] or {}
end

function __selene_tick_client_entity(entityId, delta)
    local tickEntity = clientScriptTickByEntity[entityId]
    if tickEntity then
        tickEntity(clientScriptEntityById[entityId], clientScriptDataByEntity[entityId], delta)
    end
end

function __selene_remove_client_entity(entityId)
    clientScriptDataByEntity[entityId] = nil
    clientScriptEntityById[entityId] = nil
    clientScriptTickByEntity[entityId] = nil
end
`);

      this.engine = engine;
      return engine;
    });
    return this.enginePromise;
  }
}

function getModulePath(moduleName: string): string {
  return `/client/lua/modules/${moduleName.replace(/\./g, '/')}.lua`;
}
