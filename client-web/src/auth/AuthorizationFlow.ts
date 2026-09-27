const TOKEN_KEY = 'selene_session_token';
const STATE_KEY = 'selene_oauth_state';
const VERIFIER_KEY = 'selene_oauth_verifier';
const RETURN_KEY = 'selene_oauth_return';

export async function acquireSessionToken(serverApiUrl: string): Promise<string> {
  const url = new URL(window.location.href);
  const code = url.searchParams.get('code');
  const state = url.searchParams.get('state');
  if (code || state) return exchangeCallback(serverApiUrl, code, state);

  const token = sessionStorage.getItem(TOKEN_KEY);
  if (token) return token;
  return startAuthorization(serverApiUrl);
}

export async function restartAuthorization(serverApiUrl: string): Promise<never> {
  sessionStorage.removeItem(TOKEN_KEY);
  return startAuthorization(serverApiUrl);
}

async function startAuthorization(serverApiUrl: string): Promise<never> {
  const state = randomValue(32);
  const verifier = randomValue(48);
  const redirectUri = `${window.location.origin}${window.location.pathname}`;
  sessionStorage.setItem(STATE_KEY, state);
  sessionStorage.setItem(VERIFIER_KEY, verifier);
  sessionStorage.setItem(RETURN_KEY, `${window.location.pathname}${window.location.search}${window.location.hash}`);

  const authorizationUrl = new URL('/authorize', trailingSlash(serverApiUrl));
  authorizationUrl.searchParams.set('response_type', 'code');
  authorizationUrl.searchParams.set('redirect_uri', redirectUri);
  authorizationUrl.searchParams.set('state', state);
  authorizationUrl.searchParams.set('code_challenge', await challenge(verifier));
  authorizationUrl.searchParams.set('code_challenge_method', 'S256');
  window.location.replace(authorizationUrl);
  return new Promise<never>(() => undefined);
}

async function exchangeCallback(serverApiUrl: string, code: string | null, state: string | null): Promise<string> {
  const expectedState = sessionStorage.getItem(STATE_KEY);
  const verifier = sessionStorage.getItem(VERIFIER_KEY);
  if (!code || !state || state !== expectedState || !verifier) throw new Error('Invalid OAuth callback.');

  const redirectUri = `${window.location.origin}${window.location.pathname}`;
  const response = await fetch(new URL('/token', trailingSlash(serverApiUrl)), {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'authorization_code', code, code_verifier: verifier, redirect_uri: redirectUri }),
  });
  if (!response.ok) throw new Error(`Authorization-code exchange failed (${response.status} ${response.statusText}).`);
  const result = await response.json() as { access_token?: unknown };
  if (typeof result.access_token !== 'string' || !result.access_token) throw new Error('The server returned an invalid session token.');

  sessionStorage.setItem(TOKEN_KEY, result.access_token);
  sessionStorage.removeItem(STATE_KEY);
  sessionStorage.removeItem(VERIFIER_KEY);
  const returnTo = sessionStorage.getItem(RETURN_KEY) || '/';
  sessionStorage.removeItem(RETURN_KEY);
  window.history.replaceState(null, '', returnTo);
  return result.access_token;
}

function randomValue(size: number): string {
  return base64Url(crypto.getRandomValues(new Uint8Array(size)));
}

async function challenge(verifier: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64Url(new Uint8Array(digest));
}

function base64Url(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function trailingSlash(url: string): string {
  return url.endsWith('/') ? url : `${url}/`;
}
