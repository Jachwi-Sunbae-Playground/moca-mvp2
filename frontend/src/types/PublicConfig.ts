export type PublicConfig = {
  apiBaseUrl: string;
  googleClientId: string;
  googleRedirectUri: string;
  authMode?: 'demo' | 'google';
  mapProviderMode?: 'demo' | 'kakao';
  kakaoMapJavaScriptKey?: string;
};
