import { describe, expect, it } from 'vitest';
import { consumeOAuthTransaction, getOAuthTransactionStorageKey, saveOAuthTransaction } from './oauthTransaction';

describe('OAuth 일회성 저장소', () => {
  it('PKCE 값은 한 번 읽는 즉시 sessionStorage에서 삭제한다', () => {
    const transaction = {
      codeVerifier: 'v'.repeat(43),
      state: 's'.repeat(43),
      nonce: 'n'.repeat(43),
    };

    saveOAuthTransaction(window.sessionStorage, transaction);

    expect(consumeOAuthTransaction(window.sessionStorage)).toEqual(transaction);
    expect(window.sessionStorage.getItem(getOAuthTransactionStorageKey())).toBeNull();
    expect(consumeOAuthTransaction(window.sessionStorage)).toBeNull();
  });

  it('손상된 값도 읽는 즉시 삭제한다', () => {
    window.sessionStorage.setItem(getOAuthTransactionStorageKey(), '{broken');

    expect(consumeOAuthTransaction(window.sessionStorage)).toBeNull();
    expect(window.sessionStorage.getItem(getOAuthTransactionStorageKey())).toBeNull();
  });
});
