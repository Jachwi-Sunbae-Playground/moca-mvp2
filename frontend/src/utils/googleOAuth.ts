import type { PublicConfig } from '../types/PublicConfig';
import { clearOAuthTransaction, saveOAuthTransaction } from './oauthTransaction';
import { createPkceArtifacts, type PkceArtifacts } from './pkce';

const GOOGLE_AUTHORIZATION_ENDPOINT = 'https://accounts.google.com/o/oauth2/v2/auth';

export const buildGoogleAuthorizationUrl = (config: PublicConfig, artifacts: PkceArtifacts): string => {
  const url = new URL(GOOGLE_AUTHORIZATION_ENDPOINT);
  url.searchParams.set('client_id', config.googleClientId);
  url.searchParams.set('redirect_uri', config.googleRedirectUri);
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('scope', 'openid email profile');
  url.searchParams.set('code_challenge', artifacts.codeChallenge);
  url.searchParams.set('code_challenge_method', 'S256');
  url.searchParams.set('state', artifacts.state);
  url.searchParams.set('nonce', artifacts.nonce);
  return url.toString();
};

type StartGoogleLoginDependencies = {
  storage?: Storage;
  navigate?: (url: string) => void;
  createArtifacts?: () => Promise<PkceArtifacts>;
};

export const startGoogleLogin = async (
  config: PublicConfig,
  {
    storage = window.sessionStorage,
    navigate = (url) => window.location.assign(url),
    createArtifacts = createPkceArtifacts,
  }: StartGoogleLoginDependencies = {},
) => {
  const artifacts = await createArtifacts();
  saveOAuthTransaction(storage, artifacts);

  try {
    navigate(buildGoogleAuthorizationUrl(config, artifacts));
  } catch (error) {
    clearOAuthTransaction(storage);
    throw error;
  }
};
