import { describe, expect, it } from 'vitest';
import { createCodeChallenge, createPkceArtifacts, generateSecureRandomValue } from './pkce';

describe('PKCE', () => {
  it('43자 이상의 unreserved code verifier를 생성한다', () => {
    const verifier = generateSecureRandomValue();

    expect(verifier).toHaveLength(43);
    expect(verifier).toMatch(/^[A-Za-z0-9_-]+$/);
  });

  it('RFC 7636 예시와 같은 S256 code challenge를 만든다', async () => {
    const challenge = await createCodeChallenge('dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk');

    expect(challenge).toBe('E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM');
  });

  it('state와 nonce를 각각 안전한 무작위 값으로 생성한다', async () => {
    const artifacts = await createPkceArtifacts();

    expect(artifacts.state).toMatch(/^[A-Za-z0-9_-]{43}$/);
    expect(artifacts.nonce).toMatch(/^[A-Za-z0-9_-]{43}$/);
    expect(artifacts.state).not.toBe(artifacts.nonce);
  });
});
