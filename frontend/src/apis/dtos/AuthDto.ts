import type { MemberDto } from './MemberDto';

export type GoogleLoginRequestDto = {
  authorizationCode: string;
  codeVerifier: string;
  nonce: string;
  redirectUri: string;
};

export type GoogleLoginResponseDto = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  member: MemberDto;
};
