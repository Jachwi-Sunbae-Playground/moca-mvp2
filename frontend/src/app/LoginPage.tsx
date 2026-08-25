import { type FormEvent, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError, getSafeApiErrorMessage } from '../apis/apiClient';
import { submitNicknameLogin } from '../apis/authApi';
import logo from '../assets/jachwi-sunbae-logo.png';
import Icon from '../components/ui/Icon';
import { useAuthentication } from '../hooks/useAuthentication';
import type { PublicConfig } from '../types/PublicConfig';
import { setAuthentication } from './authStore';
import styles from './LoginPage.module.css';

type LoginPageProps = {
  config: PublicConfig;
};

const getLoginErrorMessage = (error: unknown): string => {
  if (error instanceof ApiError) {
    if (error.code === 'NICKNAME_AUTHENTICATION_FAILED') {
      return '닉네임 또는 비밀번호가 맞지 않아요.';
    }
    if (error.code === 'NICKNAME_PASSWORD_UNEXPECTED') {
      return '이미 비밀번호 없이 사용하는 닉네임이에요. 비밀번호를 비우고 다시 시작해 주세요.';
    }
    if (error.code === 'NICKNAME_AUTH_RATE_LIMITED') {
      return '비밀번호를 여러 번 확인하지 못했어요. 10분 뒤 다시 시도해 주세요.';
    }
    if (error.code === 'NICKNAME_INVALID') {
      return '닉네임은 1자 이상 30자 이하로 입력해 주세요.';
    }
    if (error.code === 'NICKNAME_PASSWORD_INVALID') {
      return '비밀번호는 4자 이상 64자 이하로 입력해 주세요.';
    }
  }
  return getSafeApiErrorMessage(error);
};

const LoginPage = ({ config }: LoginPageProps) => {
  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [isStarting, setIsStarting] = useState(false);
  const [startError, setStartError] = useState<string | null>(null);
  const isStartingRef = useRef(false);
  const { terminationReason } = useAuthentication();
  const navigate = useNavigate();

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (isStartingRef.current) return;

    const trimmedNickname = nickname.trim();
    if (trimmedNickname.length === 0) {
      setStartError('이름 또는 닉네임을 입력해 주세요.');
      return;
    }
    if (password.length > 0 && password.length < 4) {
      setStartError('비밀번호는 사용하려면 4자 이상 입력해 주세요.');
      return;
    }

    isStartingRef.current = true;
    setIsStarting(true);
    setStartError(null);

    try {
      const response = await submitNicknameLogin(config, {
        nickname: trimmedNickname,
        password: password.length === 0 ? undefined : password,
      });
      setAuthentication(response);
      navigate('/properties', { replace: true });
    } catch (error) {
      isStartingRef.current = false;
      setIsStarting(false);
      setStartError(getLoginErrorMessage(error));
    }
  };

  const authenticationNotice =
    terminationReason === 'expired'
      ? '로그인 시간이 지나 다시 시작해야 해요.'
      : terminationReason === 'unauthorized'
        ? '인증을 확인하지 못해 로그아웃됐어요. 다시 시작해 주세요.'
        : null;

  return (
    <main className={styles.page}>
      <section className={styles.card} aria-labelledby="login-heading">
        <div className={styles.heroLogo}>
          <img src={logo} alt="자취선배" />
        </div>
        <header className={styles.brandHeader}>
          <p>처음 방을 보는 날부터, 떠나는 날까지.</p>
          <h1 id="login-heading">이름만으로 바로 시작해요</h1>
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

        <form className={styles.actions} aria-label="닉네임으로 시작하기" onSubmit={handleLogin}>
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
          <label className={styles.field}>
            <span>이름 또는 닉네임</span>
            <input
              autoComplete="username"
              maxLength={30}
              value={nickname}
              onChange={(event) => setNickname(event.target.value)}
              placeholder="예: 자취초보"
              disabled={isStarting}
            />
          </label>
          <label className={styles.field}>
            <span>
              비밀번호 <small>선택 · 4자 이상</small>
            </span>
            <input
              type="password"
              autoComplete="current-password"
              maxLength={64}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="기록을 보호하려면 입력하세요"
              disabled={isStarting}
            />
          </label>
          <p className={styles.sharedWarning}>
            비밀번호를 비우면 같은 닉네임을 입력한 사람이 기록을 함께 조회하고 수정할 수 있어요.
          </p>
          <button className={styles.loginButton} type="submit" disabled={isStarting}>
            <Icon name="arrow-right" size={18} />
            {isStarting ? '시작하는 중…' : '이름으로 시작하기'}
          </button>
          <p className={styles.agreement}>처음 쓰는 닉네임이면 새 기록 공간을 만들어요.</p>
        </form>
      </section>
    </main>
  );
};

export default LoginPage;
