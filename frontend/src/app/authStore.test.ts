import { describe, expect, it, vi } from 'vitest';
import {
  authenticationSessionStorageKey,
  calculateExpiresAt,
  clearAuthentication,
  getAccessToken,
  getAuthenticationSnapshot,
  restoreAuthentication,
  setAuthentication,
} from './authStore';
import { propertyQueryKeys } from './propertyQueryKeys';
import { queryClient, currentMemberQueryKey } from './queryClient';

describe('인증 저장소', () => {
  it('expiresIn을 기준으로 만료 시각을 계산한다', () => {
    expect(calculateExpiresAt(43_200, 1_000)).toBe(43_201_000);
  });

  it('토큰을 sessionStorage에 저장하고 메모리에서 읽는다', () => {
    setAuthentication({ accessToken: 'memory-token', tokenType: 'Bearer', expiresIn: 60 });

    expect(getAccessToken()).toBe('memory-token');
    expect(window.localStorage).toHaveLength(0);
    expect(JSON.parse(window.sessionStorage.getItem(authenticationSessionStorageKey) ?? '')).toMatchObject({
      accessToken: 'memory-token',
      tokenType: 'Bearer',
    });
  });

  it('새로고침 뒤 sessionStorage의 유효한 인증을 복원한다', () => {
    const expiresAt = Date.now() + 60_000;
    window.sessionStorage.setItem(
      authenticationSessionStorageKey,
      JSON.stringify({ accessToken: 'restored-token', tokenType: 'Bearer', expiresAt }),
    );

    expect(restoreAuthentication()).toEqual({
      accessToken: 'restored-token',
      tokenType: 'Bearer',
      expiresAt,
    });
    expect(getAccessToken()).toBe('restored-token');
  });

  it('만료되거나 형식이 잘못된 인증은 복원하지 않고 저장소에서 제거한다', () => {
    window.sessionStorage.setItem(
      authenticationSessionStorageKey,
      JSON.stringify({ accessToken: 'expired-token', tokenType: 'Bearer', expiresAt: Date.now() - 1 }),
    );

    expect(restoreAuthentication()).toBeNull();
    expect(window.sessionStorage.getItem(authenticationSessionStorageKey)).toBeNull();

    window.sessionStorage.setItem(authenticationSessionStorageKey, '{broken');

    expect(restoreAuthentication()).toBeNull();
    expect(window.sessionStorage.getItem(authenticationSessionStorageKey)).toBeNull();
  });

  it('로그아웃하면 메모리와 sessionStorage 인증을 함께 정리한다', () => {
    setAuthentication({ accessToken: 'member-token', tokenType: 'Bearer', expiresIn: 60 });

    clearAuthentication('logout');

    expect(getAccessToken()).toBeNull();
    expect(window.sessionStorage.getItem(authenticationSessionStorageKey)).toBeNull();
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
    expect(window.sessionStorage.getItem(authenticationSessionStorageKey)).toBeNull();
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
