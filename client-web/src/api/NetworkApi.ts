export type ClientNetworkPayload = Record<string, unknown>;
export type ClientNetworkPayloadHandler = (payload: ClientNetworkPayload) => void;

export interface NetworkApi {
  handlePayload: (payloadId: string, callback: ClientNetworkPayloadHandler) => () => void;
  sendToServer: (payloadId: string, payload?: ClientNetworkPayload) => void;
}
