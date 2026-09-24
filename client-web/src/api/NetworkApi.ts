export type ClientNetworkPayload = Record<string, unknown>;
export type ClientNetworkPayloadHandler = (payload: ClientNetworkPayload) => void;

export interface NetworkApi {
  handlePayload: (payloadId: string, callback: ClientNetworkPayloadHandler) => () => void;
  onConnected: (callback: () => void) => () => void;
  sendToServer: (payloadId: string, payload?: ClientNetworkPayload) => void;
}
