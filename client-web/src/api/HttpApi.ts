export interface HttpResponse {
  status: number;
  body: string;
  success: boolean;
}

export interface HttpApi {
  post(url: string, body?: unknown, headers?: Record<string, unknown>): Promise<HttpResponse>;
}
