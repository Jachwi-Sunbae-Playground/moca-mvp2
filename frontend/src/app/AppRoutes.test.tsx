import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { getAccessToken, setAuthentication } from './authStore';
import { queryClient } from './queryClient';
import type { PublicConfig } from '../types/PublicConfig';
import { getOAuthTransactionStorageKey, saveOAuthTransaction } from '../utils/oauthTransaction';
import AppRoutes from './AppRoutes';
import { server } from '../test/server';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  googleClientId: 'test-client.apps.googleusercontent.com',
  googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
};

const member = {
  id: 1,
  name: '이자취',
  email: 'jachwi@example.com',
};

const validState = 's'.repeat(43);
const validNonce = 'n'.repeat(43);

const successEnvelope = (data: unknown) => ({
  code: 'SUCCESS',
  message: '요청에 성공했습니다.',
  data,
});

const errorEnvelope = (code: string, message: string) => ({ code, message, errors: [] });

const renderRoutes = (path: string, options: { navigateExternally?: (url: string) => void } = {}) => {
  server.use(
    http.get(`${config.apiBaseUrl}/api/properties`, () =>
      HttpResponse.json(
        successEnvelope({
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          hasNext: false,
        }),
      ),
    ),
  );

  return render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[path]}>
          <AppRoutes config={config} storage={window.sessionStorage} navigateExternally={options.navigateExternally} />
        </MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  );
};

const saveValidTransaction = () => {
  saveOAuthTransaction(window.sessionStorage, {
    codeVerifier: 'v'.repeat(43),
    state: validState,
    nonce: validNonce,
  });
};

describe('FE-1 인증 흐름', () => {
  it('비인증 사용자에게 로그인 화면을 표시한다', () => {
    renderRoutes('/login');

    expect(screen.getByRole('img', { name: '자취선배' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '자취방 결정 가이드' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '구글로 로그인하기' })).toBeInTheDocument();
  });

  it('보호 경로에 비인증으로 접근하면 로그인 화면으로 이동한다', async () => {
    renderRoutes('/');

    expect(await screen.findByRole('button', { name: '구글로 로그인하기' })).toBeInTheDocument();
  });

  it('로그인 버튼을 누르면 PKCE 요청을 저장하고 올바른 Google URL로 이동한다', async () => {
    const navigateExternally = vi.fn();
    const user = userEvent.setup();
    renderRoutes('/login', { navigateExternally });

    await user.click(screen.getByRole('button', { name: '구글로 로그인하기' }));

    await waitFor(() => expect(navigateExternally).toHaveBeenCalledOnce());
    const firstCall = navigateExternally.mock.calls[0];
    expect(firstCall).toBeDefined();
    const googleUrl = new URL(firstCall?.[0] ?? '');
    expect(googleUrl.searchParams.get('client_id')).toBe(config.googleClientId);
    expect(googleUrl.searchParams.get('response_type')).toBe('code');
    expect(googleUrl.searchParams.get('scope')).toBe('openid email profile');
    expect(googleUrl.searchParams.get('code_challenge_method')).toBe('S256');
    expect(window.sessionStorage.getItem(getOAuthTransactionStorageKey())).not.toBeNull();
  });

  it('callback state가 일치하면 API-001과 API-002를 호출하고 앱으로 이동한다', async () => {
    let loginRequest: unknown;
    saveValidTransaction();
    server.use(
      http.post(`${config.apiBaseUrl}/api/auth/google`, async ({ request }) => {
        loginRequest = await request.json();
        return HttpResponse.json(
          successEnvelope({
            accessToken: 'issued-access-token',
            tokenType: 'Bearer',
            expiresInSeconds: 43_200,
            isNewMember: false,
            member,
          }),
        );
      }),
      http.get(`${config.apiBaseUrl}/api/members`, ({ request }) => {
        expect(request.headers.get('Authorization')).toBe('Bearer issued-access-token');
        return HttpResponse.json(successEnvelope(member));
      }),
    );

    renderRoutes(`/oauth/google/callback?code=sensitive-code&state=${validState}`);

    expect(await screen.findByRole('heading', { name: '내 매물' })).toBeInTheDocument();
    expect(loginRequest).toEqual({
      authorizationCode: 'sensitive-code',
    });
    expect(getAccessToken()).toBe('issued-access-token');
    expect(window.sessionStorage.getItem(getOAuthTransactionStorageKey())).toBeNull();
    expect(window.localStorage).toHaveLength(0);
  });

  it('callback state가 다르면 API-001을 호출하지 않고 일회성 값을 삭제한다', async () => {
    let exchangeCallCount = 0;
    saveValidTransaction();
    server.use(
      http.post(`${config.apiBaseUrl}/api/auth/google`, () => {
        exchangeCallCount += 1;
        return HttpResponse.json(successEnvelope({}));
      }),
    );

    renderRoutes(`/oauth/google/callback?code=sensitive-code&state=${'x'.repeat(43)}`);

    expect(await screen.findByText('로그인 요청을 확인할 수 없어요')).toBeInTheDocument();
    expect(exchangeCallCount).toBe(0);
    expect(window.sessionStorage.getItem(getOAuthTransactionStorageKey())).toBeNull();
  });

  it('Google 인증 취소 callback을 처리하고 일회성 값을 삭제한다', async () => {
    saveValidTransaction();

    renderRoutes(`/oauth/google/callback?error=access_denied&state=${validState}`);

    expect(await screen.findByText('Google 로그인이 취소됐어요')).toBeInTheDocument();
    expect(window.sessionStorage.getItem(getOAuthTransactionStorageKey())).toBeNull();
  });

  it('callback code가 없으면 API를 호출하지 않는다', async () => {
    let exchangeCallCount = 0;
    saveValidTransaction();
    server.use(
      http.post(`${config.apiBaseUrl}/api/auth/google`, () => {
        exchangeCallCount += 1;
        return HttpResponse.json(successEnvelope({}));
      }),
    );

    renderRoutes(`/oauth/google/callback?state=${validState}`);

    expect(await screen.findByText('로그인 정보가 도착하지 않았어요')).toBeInTheDocument();
    expect(exchangeCallCount).toBe(0);
    expect(window.sessionStorage.getItem(getOAuthTransactionStorageKey())).toBeNull();
  });

  it('API-001 실패 시 서버 내부 메시지와 민감정보를 화면에 노출하지 않는다', async () => {
    saveValidTransaction();
    server.use(
      http.post(`${config.apiBaseUrl}/api/auth/google`, () =>
        HttpResponse.json(
          errorEnvelope('GOOGLE_AUTHORIZATION_CODE_INVALID', 'authorizationCode=sensitive-code, codeVerifier=secret'),
          { status: 400 },
        ),
      ),
    );

    renderRoutes(`/oauth/google/callback?code=sensitive-code&state=${validState}`);

    expect(await screen.findByText('로그인을 완료하지 못했어요')).toBeInTheDocument();
    expect(screen.getByText('Google 인증을 확인하지 못했습니다. 로그인을 다시 시작해 주세요.')).toBeInTheDocument();
    expect(screen.queryByText(/sensitive-code|codeVerifier=secret/)).not.toBeInTheDocument();
  });

  it('저장된 유효 토큰으로 API-002를 호출해 인증 상태를 확정한다', async () => {
    setAuthentication({ accessToken: 'saved-in-memory', tokenType: 'Bearer', expiresIn: 60 });
    server.use(
      http.get(`${config.apiBaseUrl}/api/members`, ({ request }) => {
        expect(request.headers.get('Authorization')).toBe('Bearer saved-in-memory');
        return HttpResponse.json(successEnvelope(member));
      }),
    );

    renderRoutes('/');

    expect(await screen.findByRole('heading', { name: '내 매물' })).toBeInTheDocument();
  });

  it('API-002가 401이면 메모리 토큰을 지우고 로그인으로 이동한다', async () => {
    setAuthentication({ accessToken: 'expired-token', tokenType: 'Bearer', expiresIn: 60 });
    server.use(
      http.get(`${config.apiBaseUrl}/api/members`, () =>
        HttpResponse.json(errorEnvelope('ACCESS_TOKEN_EXPIRED', 'expired'), { status: 401 }),
      ),
    );

    renderRoutes('/');

    expect(await screen.findByRole('button', { name: '구글로 로그인하기' })).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
    expect(screen.getByText(/인증을 확인하지 못해 로그아웃/)).toBeInTheDocument();
  });

  it('API-002 네트워크 실패는 토큰 만료로 오인하지 않고 재시도를 제공한다', async () => {
    setAuthentication({ accessToken: 'still-valid-token', tokenType: 'Bearer', expiresIn: 60 });
    server.use(http.get(`${config.apiBaseUrl}/api/members`, () => HttpResponse.error()));

    renderRoutes('/');

    expect(await screen.findByText('회원 정보를 불러오지 못했어요')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '다시 시도하기' })).toBeInTheDocument();
    expect(getAccessToken()).toBe('still-valid-token');
  });

  it('인증 사용자가 로그인 경로에 접근하면 앱 시작 경로로 이동한다', async () => {
    setAuthentication({ accessToken: 'valid-token', tokenType: 'Bearer', expiresIn: 60 });
    server.use(http.get(`${config.apiBaseUrl}/api/members`, () => HttpResponse.json(successEnvelope(member))));

    renderRoutes('/login');

    expect(await screen.findByRole('heading', { name: '내 매물' })).toBeInTheDocument();
  });

  it('로그아웃하면 인증 정보와 회원 캐시를 지우고 로그인으로 이동한다', async () => {
    const user = userEvent.setup();
    setAuthentication({ accessToken: 'valid-token', tokenType: 'Bearer', expiresIn: 60 });
    server.use(http.get(`${config.apiBaseUrl}/api/members`, () => HttpResponse.json(successEnvelope(member))));
    renderRoutes('/');

    await user.click(await screen.findByRole('link', { name: '마이' }));
    await screen.findByRole('heading', { name: '마이' });
    await user.click(screen.getByRole('button', { name: '로그아웃' }));

    expect(await screen.findByRole('button', { name: '구글로 로그인하기' })).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
  });

  it.each([
    ['/compare', '매물 비교는 준비 중이에요'],
    ['/export', '기록 내보내기는 준비 중이에요'],
    ['/tips', '선배 팁은 준비 중이에요'],
  ])('%s는 실제 기능 대신 공통 준비 중 안내를 표시한다', async (path, heading) => {
    setAuthentication({ accessToken: 'valid-token', tokenType: 'Bearer', expiresIn: 60 });
    server.use(http.get(`${config.apiBaseUrl}/api/members`, () => HttpResponse.json(successEnvelope(member))));

    renderRoutes(path);

    expect(await screen.findByRole('heading', { name: heading, level: 1 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '1차 MVP에서는 안내만 제공해요' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '내 매물 보기' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '체크리스트 보기' })).toBeInTheDocument();
  });

  it('알 수 없는 경로에 fallback을 표시한다', () => {
    renderRoutes('/does-not-exist');

    expect(screen.getByText('페이지를 찾을 수 없어요')).toBeInTheDocument();
  });
});
