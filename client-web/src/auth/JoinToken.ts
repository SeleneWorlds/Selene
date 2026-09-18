export const JOIN_TOKEN_COOKIE_NAME = 'selene_join_token';

export function readJoinToken(cookieHeader: string = document.cookie): string {
  for (const cookie of cookieHeader.split(';')) {
    const separatorIndex = cookie.indexOf('=');
    const name = separatorIndex === -1 ? cookie.trim() : cookie.slice(0, separatorIndex).trim();

    if (name === JOIN_TOKEN_COOKIE_NAME && separatorIndex !== -1) {
      const token = decodeURIComponent(cookie.slice(separatorIndex + 1));
      if (token.trim()) {
        return token;
      }
    }
  }

  return createOfflineToken();
}

export function createOfflineToken(): string {
  const header = encodeJwtPart({ typ: 'JWT', alg: 'none' });
  const payload = encodeJwtPart({ sub: 'unauthenticated-user' });
  return `${header}.${payload}.`;
}

function encodeJwtPart(value: object): string {
  return btoa(JSON.stringify(value))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}
