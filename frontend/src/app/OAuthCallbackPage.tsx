import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ApiError, getSafeApiErrorMessage } from '../apis/apiClient';
import { submitGoogleLogin } from '../apis/authApi';
import { setAuthentication } from './authStore';
import { getCurrentMemberQueryOptions } from '../hooks/query/useCurrentMember';
import StatusPanel from '../components/StatusPanel';
import { Button, ButtonLink } from '../components/ui/Button';
import type { PublicConfig } from '../types/PublicConfig';
import { consumeOAuthTransaction } from '../utils/oauthTransaction';
import { queryClient } from './queryClient';

type CallbackStep =
  | { kind: 'processing' }
  | { kind: 'cancelled' }
  | { kind: 'invalid-state' }
  | { kind: 'missing-code' }
  | { kind: 'exchange-failed'; message: string }
  | { kind: 'member-failed'; message: string }
  | { kind: 'success' };

type OAuthCallbackPageProps = {
  config: PublicConfig;
  storage?: Storage;
};

const OAuthCallbackPage = ({ config, storage = window.sessionStorage }: OAuthCallbackPageProps) => {
  const location = useLocation();
  const navigate = useNavigate();
  const initialSearchRef = useRef(location.search);
  const hasProcessedRef = useRef(false);
  const abortControllerRef = useRef<AbortController | null>(null);
  const cleanupTimerRef = useRef<number | null>(null);
  const successNavigationTimerRef = useRef<number | null>(null);
  const [step, setStep] = useState<CallbackStep>({ kind: 'processing' });

  const scheduleCleanup = () => {
    cleanupTimerRef.current = window.setTimeout(() => {
      abortControllerRef.current?.abort();

      if (successNavigationTimerRef.current !== null) {
        window.clearTimeout(successNavigationTimerRef.current);
      }
    }, 0);
  };

  const confirmCurrentMember = async () => {
    try {
      await queryClient.fetchQuery(getCurrentMemberQueryOptions(config));
      setStep({ kind: 'success' });

      const shouldReduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
      successNavigationTimerRef.current = window.setTimeout(
        () => navigate('/properties', { replace: true }),
        shouldReduceMotion ? 0 : 450,
      );
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        navigate('/login', { replace: true });
        return;
      }

      setStep({ kind: 'member-failed', message: getSafeApiErrorMessage(error) });
    }
  };

  useEffect(() => {
    if (cleanupTimerRef.current !== null) {
      window.clearTimeout(cleanupTimerRef.current);
      cleanupTimerRef.current = null;
    }

    if (hasProcessedRef.current) {
      return scheduleCleanup;
    }

    hasProcessedRef.current = true;
    const abortController = new AbortController();
    abortControllerRef.current = abortController;
    const searchParams = new URLSearchParams(initialSearchRef.current);
    const authorizationCode = searchParams.get('code');
    const callbackState = searchParams.get('state');
    const googleError = searchParams.get('error');
    const transaction = consumeOAuthTransaction(storage);

    navigate(location.pathname, { replace: true });

    if (googleError !== null) {
      setStep(
        googleError === 'access_denied'
          ? { kind: 'cancelled' }
          : {
              kind: 'exchange-failed',
              message: 'Google 로그인을 완료하지 못했습니다. 다시 시도해 주세요.',
            },
      );
      return scheduleCleanup;
    }

    if (authorizationCode === null || authorizationCode.length === 0) {
      setStep({ kind: 'missing-code' });
      return scheduleCleanup;
    }

    if (transaction === null || callbackState === null || callbackState !== transaction.state) {
      setStep({ kind: 'invalid-state' });
      return scheduleCleanup;
    }

    const exchangeCode = async () => {
      try {
        const response = await submitGoogleLogin(
          config,
          {
            authorizationCode,
            codeVerifier: transaction.codeVerifier,
            nonce: transaction.nonce,
            redirectUri: config.googleRedirectUri,
          },
          abortController.signal,
        );

        setAuthentication(response);
        await confirmCurrentMember();
      } catch (error) {
        if (error instanceof Error && error.name === 'AbortError') {
          return;
        }

        setStep({ kind: 'exchange-failed', message: getSafeApiErrorMessage(error) });
      }
    };

    void exchangeCode();
    return scheduleCleanup;
  }, [config, location.pathname, navigate, storage]);

  if (step.kind === 'processing') {
    return (
      <StatusPanel
        title="로그인을 확인하고 있어요"
        description="잠시만 기다려 주세요. 인증 정보는 화면이나 로그에 남기지 않아요."
        isBusy
      />
    );
  }

  if (step.kind === 'success') {
    return <StatusPanel title="로그인했어요" description="자취선배 시작 화면으로 이동합니다." tone="success" isBusy />;
  }

  if (step.kind === 'cancelled') {
    return (
      <StatusPanel
        title="Google 로그인이 취소됐어요"
        description="원할 때 다시 로그인할 수 있어요."
        action={<ButtonLink to="/login">로그인 화면으로 돌아가기</ButtonLink>}
      />
    );
  }

  if (step.kind === 'invalid-state') {
    return (
      <StatusPanel
        title="로그인 요청을 확인할 수 없어요"
        description="안전을 위해 로그인을 중단했습니다. 로그인 화면에서 다시 시작해 주세요."
        tone="error"
        action={<ButtonLink to="/login">로그인 다시 시작하기</ButtonLink>}
      />
    );
  }

  if (step.kind === 'missing-code') {
    return (
      <StatusPanel
        title="로그인 정보가 도착하지 않았어요"
        description="Google 로그인 화면에서 다시 인증해 주세요."
        tone="error"
        action={<ButtonLink to="/login">로그인 다시 시작하기</ButtonLink>}
      />
    );
  }

  if (step.kind === 'member-failed') {
    return (
      <StatusPanel
        title="회원 정보를 불러오지 못했어요"
        description={step.message}
        tone="error"
        action={<Button onClick={() => void confirmCurrentMember()}>다시 확인하기</Button>}
      />
    );
  }

  return (
    <StatusPanel
      title="로그인을 완료하지 못했어요"
      description={step.message}
      tone="error"
      action={<ButtonLink to="/login">로그인 다시 시작하기</ButtonLink>}
    />
  );
};

export default OAuthCallbackPage;
