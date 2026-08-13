const base64UrlEncode = (bytes: Uint8Array): string => {
  let binary = '';

  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });

  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
};

export const generateSecureRandomValue = (byteLength = 32): string => {
  const bytes = new Uint8Array(byteLength);
  crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes);
};

export const createCodeChallenge = async (codeVerifier: string): Promise<string> => {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(codeVerifier));
  return base64UrlEncode(new Uint8Array(digest));
};

export type PkceArtifacts = {
  codeVerifier: string;
  codeChallenge: string;
  state: string;
  nonce: string;
};

export const createPkceArtifacts = async (): Promise<PkceArtifacts> => {
  const codeVerifier = generateSecureRandomValue();

  return {
    codeVerifier,
    codeChallenge: await createCodeChallenge(codeVerifier),
    state: generateSecureRandomValue(),
    nonce: generateSecureRandomValue(),
  };
};
