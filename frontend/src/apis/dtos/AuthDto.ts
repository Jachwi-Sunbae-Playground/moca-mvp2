import type { MemberDto } from './MemberDto';

export type GoogleLoginRequestDto = {
  authorizationCode: string;
};

export type GoogleLoginResponseDto = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  isNewMember: boolean;
  member: MemberDto;
};
