import type { LoginResponseDto } from './dtos/AuthDto';
import type { MemberDto } from './dtos/MemberDto';

const isRecord = (value: unknown): value is Record<string, unknown> => typeof value === 'object' && value !== null;

const isMemberDto = (value: unknown): value is MemberDto =>
  isRecord(value) &&
  typeof value.memberId === 'number' &&
  Number.isInteger(value.memberId) &&
  value.memberId > 0 &&
  typeof value.displayName === 'string' &&
  value.displayName.length > 0 &&
  typeof value.passwordProtected === 'boolean';

const parseMember = (value: unknown): MemberDto => {
  if (!isRecord(value)) throw new Error('현재 회원 응답 형식이 올바르지 않습니다.');
  const member = { memberId: value.id, displayName: value.name, passwordProtected: value.passwordProtected };
  if (!isMemberDto(member)) throw new Error('현재 회원 응답 형식이 올바르지 않습니다.');
  return member;
};

const parseLoginMember = (value: unknown): MemberDto => {
  if (!isRecord(value)) throw new Error('로그인 회원 응답 형식이 올바르지 않습니다.');
  const member = { memberId: value.memberId, displayName: value.name, passwordProtected: value.passwordProtected };
  if (!isMemberDto(member)) throw new Error('로그인 회원 응답 형식이 올바르지 않습니다.');
  return member;
};

export const parseMemberDto = (value: unknown): MemberDto => parseMember(value);

export const parseLoginResponseDto = (value: unknown): LoginResponseDto => {
  if (
    !isRecord(value) ||
    typeof value.accessToken !== 'string' ||
    value.accessToken.length === 0 ||
    value.tokenType !== 'Bearer' ||
    typeof value.expiresIn !== 'number' ||
    !Number.isFinite(value.expiresIn) ||
    value.expiresIn <= 0
  ) {
    throw new Error('로그인 응답 형식이 올바르지 않습니다.');
  }

  return {
    accessToken: value.accessToken,
    tokenType: value.tokenType,
    expiresIn: value.expiresIn,
    member: parseLoginMember(value.member),
  };
};
