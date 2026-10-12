import { LuaFactory } from 'wasmoon';
import luaWasmUrl from 'wasmoon/dist/glue.wasm?url';

export interface LuaModuleSource {
  module: string;
  source: string;
}

export class LuaRuntime {
  private readonly factory = new LuaFactory(luaWasmUrl);
  private enginePromise: ReturnType<LuaFactory['createEngine']> | null = null;

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

      return engine;
    });
    return this.enginePromise;
  }
}

function getModulePath(moduleName: string): string {
  return `/client/lua/modules/${moduleName.replace(/\./g, '/')}.lua`;
}
