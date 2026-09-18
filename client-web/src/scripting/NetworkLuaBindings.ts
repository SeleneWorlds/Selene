import type { NetworkApi } from '@/api/NetworkApi';
import { LuaArguments } from './LuaArguments';
import type { LuaRuntime } from './LuaRuntime';

export async function registerNetworkLuaModule(runtime: LuaRuntime, network: NetworkApi): Promise<void> {
  const handlePayload = (payloadIdValue: unknown, callbackValue: unknown) => {
    const args = new LuaArguments('selene.network.handlePayload');

    network.handlePayload(
      args.string(payloadIdValue, 'payloadId'),
      args.function(callbackValue, 'callback'),
    );
  };
  const sendToServer = (payloadIdValue: unknown, payloadValue?: unknown) => {
    const args = new LuaArguments('selene.network.sendToServer');
    const payload = payloadValue === undefined || payloadValue === null
      ? {}
      : args.serializedMap(payloadValue, 'payload');

    network.sendToServer(args.string(payloadIdValue, 'payloadId'), payload);
  };

  await runtime.preloadModule('selene.network', () => ({
    handlePayload,
    sendToServer,
  }));
}
