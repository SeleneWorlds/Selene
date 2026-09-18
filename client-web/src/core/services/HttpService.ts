import type { HttpApi } from '@/api/HttpApi';

export class HttpService implements HttpApi {
  async post(url: string, body?: unknown, headers?: Record<string, unknown>) {
    const response = await fetch(url, {
      method: 'POST',
      headers: Object.fromEntries(
        Object.entries(headers ?? {}).map(([key, value]) => [key, String(value)]),
      ),
      body: body == null ? null : typeof body === 'string' ? body : JSON.stringify(body),
    });
    return {
      status: response.status,
      body: await response.text(),
      success: response.ok,
    };
  }
}
