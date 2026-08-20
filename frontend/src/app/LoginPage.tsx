import { useRef, useState } from 'react';
import logo from '../assets/jachwi-sunbae-logo.png';
import GoogleIcon from '../components/GoogleIcon';
import Icon from '../components/ui/Icon';
import type { PublicConfig } from '../types/PublicConfig';
import { startGoogleLogin } from '../utils/googleOAuth';
import { useAuthentication } from '../hooks/useAuthentication';
import styles from './LoginPage.module.css';

type LoginPageProps = {
  config: PublicConfig;
  storage?: Storage;
  navigateExternally?: (url: string) => void;
};

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
        <div className={styles.heroLogo}>
          <img src={logo} alt="자취선배" />
        </div>
        <header className={styles.brandHeader}>
          <p>처음 방을 보는 날부터, 떠나는 날까지.</p>
          <h1 id="login-heading">자취방 결정 가이드</h1>
        </header>

        <section className={styles.guideCard} aria-label="자취선배 주요 기능">
          <p>
            방마다 체크한 내용을 그대로 남기고
            <br />
            한번에 비교해서 고르세요.
          </p>
        </section>

        <ul className={styles.featureList}>
          <li>
            <span>
              <Icon name="checklist" size={16} />
            </span>
            <strong>빠짐없이 체크</strong>
          </li>
          <li>
            <span>
              <Icon name="arrow-right" size={16} />
            </span>
            <strong>매물끼리 비교</strong>
          </li>
          <li>
            <span>
              <Icon name="link" size={16} />
            </span>
            <strong>기록 자동 보관</strong>
          </li>
        </ul>

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
            {isRedirecting ? 'Google로 이동 중…' : '구글로 로그인하기'}
          </button>
          <p className={styles.agreement}>계속하면 이용약관과 개인정보처리방침에 동의하는 것으로 간주됩니다.</p>
        </section>
      </section>
    </main>
  );
};

export default LoginPage;
