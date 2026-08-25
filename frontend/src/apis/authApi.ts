import type { PublicConfig } from '../types/PublicConfig';
import { apiRequest } from './apiClient';
import { parseLoginResponseDto } from './dtoParsers';
import type { LoginResponseDto, NicknameLoginRequestDto } from './dtos/AuthDto';

export const submitNicknameLogin = (
  config: PublicConfig,
  request: NicknameLoginRequestDto,
  signal?: AbortSignal,
): Promise<LoginResponseDto> =>
  apiRequest({
    config,
    path: '/api/auth/nickname',
    method: 'POST',
    body: request,
    signal,
    requiresAuthentication: false,
    parseData: parseLoginResponseDto,
  });
