import { describe, expect, it, vi } from 'vitest';
import { calculateExpiresAt, getAccessToken, getAuthenticationSnapshot, setAuthentication } from './authStore';
import { propertyQueryKeys } from './propertyQueryKeys';
import { queryClient, currentMemberQueryKey } from './queryClient';

describe('탭 단위 인증 저장소', () => {
  it('expiresIn을 기준으로 만료 시각을 계산한다', () => {
    expect(calculateExpiresAt(43_200, 1_000)).toBe(43_201_000);
  });

  it('토큰을 localStorage에는 쓰지 않고 탭 단위 sessionStorage에서 복원할 수 있게 저장한다', () => {
    setAuthentication({ accessToken: 'memory-token', tokenType: 'Bearer', expiresIn: 60 });

    expect(getAccessToken()).toBe('memory-token');
    expect(window.localStorage).toHaveLength(0);
    expect(window.sessionStorage).toHaveLength(1);
  });

  it('만료되면 인증과 인증 Query Cache를 정리한다', () => {
    vi.useFakeTimers();
    queryClient.setQueryData(currentMemberQueryKey, { memberId: 1 });
    queryClient.setQueryData(propertyQueryKeys.detail(10), { name: '다른 회원에게 남기면 안 되는 매물' });
    setAuthentication({ accessToken: 'short-token', tokenType: 'Bearer', expiresIn: 1 });

    vi.advanceTimersByTime(1_001);

    expect(getAuthenticationSnapshot()).toEqual({
      session: null,
      terminationReason: 'expired',
    });
    expect(queryClient.getQueryData(currentMemberQueryKey)).toBeUndefined();
    expect(queryClient.getQueryData(propertyQueryKeys.detail(10))).toBeUndefined();
    vi.useRealTimers();
  });

  it('다른 인증으로 교체할 때 이전 회원의 Query Cache를 재사용하지 않는다', () => {
    setAuthentication({ accessToken: 'member-a-token', tokenType: 'Bearer', expiresIn: 60 });
    queryClient.setQueryData(currentMemberQueryKey, { memberId: 1 });
    queryClient.setQueryData(propertyQueryKeys.detail(10), { name: '첫 회원 매물' });

    setAuthentication({ accessToken: 'member-b-token', tokenType: 'Bearer', expiresIn: 60 });

    expect(getAccessToken()).toBe('member-b-token');
    expect(queryClient.getQueryData(currentMemberQueryKey)).toBeUndefined();
    expect(queryClient.getQueryData(propertyQueryKeys.detail(10))).toBeUndefined();
  });
});
