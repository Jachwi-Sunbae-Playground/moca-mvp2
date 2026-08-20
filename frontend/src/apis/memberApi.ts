import type { Member } from '../types/Member';
import type { PublicConfig } from '../types/PublicConfig';
import { apiRequest } from './apiClient';
import { parseMemberDto } from './dtoParsers';

export const fetchCurrentMember = (config: PublicConfig, signal?: AbortSignal): Promise<Member> =>
  apiRequest({
    config,
    path: '/api/members',
    signal,
    parseData: parseMemberDto,
  });
