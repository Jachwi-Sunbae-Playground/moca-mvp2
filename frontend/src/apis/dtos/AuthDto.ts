import type { MemberDto } from './MemberDto';

export type NicknameLoginRequestDto = {
  nickname: string;
  password?: string;
};

export type LoginResponseDto = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  member: MemberDto;
};
