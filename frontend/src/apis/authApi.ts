import type { PublicConfig } from '../types/PublicConfig';
import { apiRequest } from './apiClient';
import { parseGoogleLoginResponseDto } from './dtoParsers';
import type { GoogleLoginRequestDto, GoogleLoginResponseDto } from './dtos/AuthDto';

export const submitGoogleLogin = (
  config: PublicConfig,
  request: GoogleLoginRequestDto,
  signal?: AbortSignal,
): Promise<GoogleLoginResponseDto> =>
  apiRequest({
    config,
    path: '/api/auth/google',
    method: 'POST',
    body: request,
    signal,
    requiresAuthentication: false,
    parseData: parseGoogleLoginResponseDto,
  });
