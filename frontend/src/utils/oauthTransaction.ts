import type { PkceArtifacts } from './pkce';

const OAUTH_TRANSACTION_KEY = 'jachwi-sunbae.oauth-transaction';

export type OAuthTransaction = Pick<PkceArtifacts, 'codeVerifier' | 'state' | 'nonce'>;

const isOAuthTransaction = (value: unknown): value is OAuthTransaction => {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  return (
    'codeVerifier' in value &&
    typeof value.codeVerifier === 'string' &&
    /^[A-Za-z0-9._~-]{43,128}$/.test(value.codeVerifier) &&
    'state' in value &&
    typeof value.state === 'string' &&
    /^[A-Za-z0-9_-]{43}$/.test(value.state) &&
    'nonce' in value &&
    typeof value.nonce === 'string' &&
    /^[A-Za-z0-9_-]{43}$/.test(value.nonce)
  );
};

export const saveOAuthTransaction = (storage: Storage, transaction: OAuthTransaction) => {
  storage.setItem(OAUTH_TRANSACTION_KEY, JSON.stringify(transaction));
};

export const consumeOAuthTransaction = (storage: Storage): OAuthTransaction | null => {
  const serialized = storage.getItem(OAUTH_TRANSACTION_KEY);
  storage.removeItem(OAUTH_TRANSACTION_KEY);

  if (serialized === null) {
    return null;
  }

  try {
    const value: unknown = JSON.parse(serialized);
    return isOAuthTransaction(value) ? value : null;
  } catch {
    return null;
  }
};

export const clearOAuthTransaction = (storage: Storage) => {
  storage.removeItem(OAUTH_TRANSACTION_KEY);
};

export const getOAuthTransactionStorageKey = () => OAUTH_TRANSACTION_KEY;
