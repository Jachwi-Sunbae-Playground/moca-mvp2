import { useRef, useState } from 'react';
import jachwiSunbaeLogo from '../assets/jachwi-sunbae-landing-logo.png';
import checklistListScreenshot from '../assets/landing/checklist-list.jpg';
import propertyDetailScreenshot from '../assets/landing/property-detail.jpg';
import propertyListScreenshot from '../assets/landing/property-list.jpg';
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

type LandingScreenshotProps = {
  src: string;
  alt: string;
};

const LandingScreenshot = ({ src, alt }: LandingScreenshotProps) => (
  <div className={styles.screenshotFrame}>
    <img src={src} alt={alt} />
  </div>
);

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
          <div className={styles.logoViewport}>
            <img className={styles.brandLogo} src={jachwiSunbaeLogo} alt="자취선배" />
          </div>
          <h1 id="login-heading" className={styles.brandMessage} aria-label="처음 방을 보는 날부터, 떠나는 날까지">
            처음 방을 보는 날부터,
            <br />
            떠나는 날까지
          </h1>
        </header>

        <section className={styles.featureIntro} aria-labelledby="feature-heading">
          <div className={styles.featureHeading}>
            <h2 id="feature-heading">방을 볼 때 놓치기 쉬운 것은?</h2>
            <p>방을 알아보는 순간마다 필요한 기록을 한곳에 모아요.</p>
          </div>

          <div className={styles.featureList}>
            <article className={styles.featureItem}>
              <LandingScreenshot src={propertyListScreenshot} alt="기록한 매물 목록 화면" />
              <div className={styles.featureCopy}>
                <span>매물 기록·비교</span>
                <h3>본 매물을 한눈에 모아요</h3>
                <p>보증금과 월세를 기록하고, 방문 확인 진행 상황을 매물별로 비교할 수 있어요.</p>
              </div>
            </article>

            <article className={`${styles.featureItem} ${styles.featureItemReverse}`}>
              <LandingScreenshot src={propertyDetailScreenshot} alt="매물 상세 정보와 퀵 메모 화면" />
              <div className={styles.featureCopy}>
                <span>사진·정보·메모</span>
                <h3>매물 정보를 한곳에 남겨요</h3>
                <p>사진과 가격, 확인한 곳을 정리하고 현장에서 떠오른 내용도 바로 메모할 수 있어요.</p>
              </div>
            </article>

            <article className={styles.featureItem}>
              <LandingScreenshot src={checklistListScreenshot} alt="단계별 체크리스트 목록 화면" />
              <div className={styles.featureCopy}>
                <span>단계별 체크리스트</span>
                <h3>단계마다 놓치지 않게 확인해요</h3>
                <p>온라인 문의, 집에서 확인할 내용, 계약 전 조건을 나누어 확인하고 결과를 기록할 수 있어요.</p>
              </div>
            </article>
          </div>
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
            {isRedirecting ? 'Google로 이동 중…' : 'Google로 로그인하고 이용하기'}
          </button>
          <p className={styles.agreement}>계속하면 이용약관과 개인정보처리방침에 동의하는 것으로 간주됩니다.</p>
        </section>
      </section>
    </main>
  );
};

export default LoginPage;
