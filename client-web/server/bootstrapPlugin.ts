import type { IncomingMessage, ServerResponse } from 'node:http';
import { TLSSocket } from 'node:tls';
import type { Connect, Plugin } from 'vite';

const COOKIE_NAME = 'selene_join_token';
const MAX_BODY_BYTES = 64 * 1024;

export function bootstrapPlugin(): Plugin {
  const install = (middlewares: Connect.Server): void => {
    middlewares.use('/bootstrap', handleBootstrap);
  };

  return {
    name: 'selene-bootstrap-endpoint',
    configureServer(server) {
      install(server.middlewares);
    },
    configurePreviewServer(server) {
      install(server.middlewares);
    },
  };
}

async function handleBootstrap(request: IncomingMessage, response: ServerResponse): Promise<void> {
  if (request.method !== 'POST') {
    response.statusCode = 405;
    response.setHeader('Allow', 'POST');
    response.end('Method Not Allowed');
    return;
  }

  try {
    const token = extractToken(await readBody(request), request.headers['content-type']);
    if (!token) {
      response.statusCode = 400;
      response.end('A non-empty token field is required.');
      return;
    }

    const secure = request.headers['x-forwarded-proto'] === 'https' || request.socket instanceof TLSSocket;
    response.setHeader(
      'Set-Cookie',
      `${COOKIE_NAME}=${encodeURIComponent(token)}; Path=/; SameSite=Strict${secure ? '; Secure' : ''}`,
    );
    response.statusCode = 303;
    response.setHeader('Location', '/');
    response.end();
  } catch (error) {
    response.statusCode = error instanceof BodyTooLargeError ? 413 : 400;
    response.end(error instanceof Error ? error.message : 'Invalid bootstrap request.');
  }
}

async function readBody(request: IncomingMessage): Promise<string> {
  const chunks: Buffer[] = [];
  let size = 0;

  for await (const chunk of request) {
    const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
    size += buffer.length;
    if (size > MAX_BODY_BYTES) {
      throw new BodyTooLargeError();
    }
    chunks.push(buffer);
  }

  return Buffer.concat(chunks).toString('utf8');
}

function extractToken(body: string, contentType: string | undefined): string | null {
  if (contentType?.includes('application/json')) {
    const value = (JSON.parse(body) as { token?: unknown }).token;
    return typeof value === 'string' && value.trim() ? value : null;
  }

  const value = new URLSearchParams(body).get('token');
  return value?.trim() ? value : null;
}

class BodyTooLargeError extends Error {
  constructor() {
    super('Bootstrap request body is too large.');
  }
}
