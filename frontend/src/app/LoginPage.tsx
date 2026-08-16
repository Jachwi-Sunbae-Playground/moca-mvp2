import { useRef, useState } from 'react';
import jachwiSunbaeLogo from '../assets/jachwi-sunbae-logo.png';
import FeatureIcon from '../components/FeatureIcon';
import GoogleIcon from '../components/GoogleIcon';
import type { PublicConfig } from '../types/PublicConfig';
import { startGoogleLogin } from '../utils/googleOAuth';
import { useAuthentication } from '../hooks/useAuthentication';
import styles from './LoginPage.module.css';

type LoginPageProps = {
  config: PublicConfig;
  storage?: Storage;
  navigateExternally?: (url: string) => void;
};

const features = [
  {
    icon: 'check' as const,
    title: '빠짐없이 체크',
    description: '방마다 확인할 내용을 놓치지 않아요.',
  },
  {
    icon: 'compare' as const,
    title: '방문마다 기록',
    description: '괜찮음·주의·미확인 결과를 다시 확인해요.',
  },
  {
    icon: 'archive' as const,
    title: '기록 자동 보관',
    description: '처음 본 날부터 결정의 근거를 남겨요.',
  },
];

const LoginPage = ({ config, storage, navigateExternally }: LoginPageProps) => {
  const [isRedirecting, setIsRedirecting] = useState(false);
  const [startError, setStartError] = useState<string | null>(null);
  const isStartingRef = useRef(false);
  const { terminationReason } = useAuthentication();

  const handleLogin = async () => {
    if (isStartingRef.current) {
      return;
    }

    isStartingRef.current = true;
    setIsRedirecting(true);
    setStartError(null);

    try {
      await startGoogleLogin(config, {
        storage,
        navigate: navigateExternally,
      });
    } catch {
      isStartingRef.current = false;
      setIsRedirecting(false);
      setStartError('로그인을 시작하지 못했습니다. 잠시 후 다시 시도해 주세요.');
    }
  };

  const authenticationNotice =
    terminationReason === 'expired'
      ? 'Access Token이 만료되어 다시 로그인이 필요합니다.'
      : terminationReason === 'unauthorized'
        ? '인증을 확인하지 못해 로그아웃되었습니다. 다시 로그인해 주세요.'
        : null;

  return (
    <main className={styles.page}>
      <section className={styles.card} aria-labelledby="login-heading">
        <header className={styles.brandHeader}>
          <img
            className={styles.brandLogo}
            src={jachwiSunbaeLogo}
            alt="자취의 모든 순간, 믿고 찾는 든든한 선배 자취선배"
          />
          <p className={styles.brandMessage}>처음 방을 보는 날부터, 떠나는 날까지.</p>
        </header>

        <section className={styles.guideSection} aria-labelledby="login-heading">
          <h1 id="login-heading">자취방 결정 가이드</h1>
          <p className={styles.guideSummary}>
            방문마다 체크 결과를 그대로 남기고
            <br />
            결정의 근거로 다시 확인하세요.
          </p>

          <ul className={styles.featureList}>
            {features.map((feature) => (
              <li key={feature.title}>
                <span className={styles.featureIcon}>
                  <FeatureIcon name={feature.icon} />
                </span>
                <span>
                  <strong>{feature.title}</strong>
                  <span className={styles.featureDescription}>{feature.description}</span>
                </span>
              </li>
            ))}
          </ul>
        </section>

        <section className={styles.actions} aria-label="로그인">
          {authenticationNotice === null ? null : (
            <p className={styles.notice} role="status">
              {authenticationNotice}
            </p>
          )}
          {startError === null ? null : (
            <p className={styles.error} role="alert">
              {startError}
            </p>
          )}
          <button className={styles.googleLoginButton} type="button" onClick={handleLogin} disabled={isRedirecting}>
            <GoogleIcon className={styles.googleIcon} />
            {isRedirecting ? 'Google로 이동 중…' : 'Google로 로그인하기'}
          </button>
          <p className={styles.sessionNote}>새로고침하거나 새 탭을 열면 다시 로그인해야 해요.</p>
          <p className={styles.legalNote}>
            계속하면 이용약관과 개인정보처리방침에 동의하는 것으로 간주됩니다.
            <span>약관 문서는 준비 중이며, 확정 후 연결됩니다.</span>
          </p>
        </section>
      </section>
    </main>
  );
};

export default LoginPage;
