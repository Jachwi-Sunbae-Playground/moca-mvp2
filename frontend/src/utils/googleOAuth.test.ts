import { describe, expect, it, vi } from 'vitest';
import type { PublicConfig } from '../types/PublicConfig';
import { getOAuthTransactionStorageKey } from './oauthTransaction';
import { buildGoogleAuthorizationUrl, startGoogleLogin } from './googleOAuth';
import type { PkceArtifacts } from './pkce';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  googleClientId: 'test-client.apps.googleusercontent.com',
  googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
};

const artifacts: PkceArtifacts = {
  codeVerifier: 'v'.repeat(43),
  codeChallenge: 'challenge',
  state: 's'.repeat(43),
  nonce: 'n'.repeat(43),
};

describe('Google OAuth 시작', () => {
  it('Authorization Code + PKCE 요청 파라미터를 정확히 만든다', () => {
    const url = new URL(buildGoogleAuthorizationUrl(config, artifacts));

    expect(url.origin + url.pathname).toBe('https://accounts.google.com/o/oauth2/v2/auth');
    expect(Object.fromEntries(url.searchParams)).toEqual({
      client_id: config.googleClientId,
      redirect_uri: config.googleRedirectUri,
      response_type: 'code',
      scope: 'openid email profile',
      code_challenge: artifacts.codeChallenge,
      code_challenge_method: 'S256',
      state: artifacts.state,
      nonce: artifacts.nonce,
    });
  });

  it('일회성 값만 저장한 뒤 Google로 이동한다', async () => {
    const navigate = vi.fn();

    await startGoogleLogin(config, {
      storage: window.sessionStorage,
      navigate,
      createArtifacts: async () => artifacts,
    });

    expect(navigate).toHaveBeenCalledOnce();
    expect(window.sessionStorage.getItem(getOAuthTransactionStorageKey())).toContain(artifacts.codeVerifier);
    expect(window.sessionStorage.getItem(getOAuthTransactionStorageKey())).not.toContain('accessToken');
    expect(window.localStorage).toHaveLength(0);
  });

  it('외부 이동에 실패하면 저장했던 일회성 값을 정리한다', async () => {
    await expect(
      startGoogleLogin(config, {
        storage: window.sessionStorage,
        navigate: () => {
          throw new Error('navigation failed');
        },
        createArtifacts: async () => artifacts,
      }),
    ).rejects.toThrow('navigation failed');

    expect(window.sessionStorage.getItem(getOAuthTransactionStorageKey())).toBeNull();
  });
});
