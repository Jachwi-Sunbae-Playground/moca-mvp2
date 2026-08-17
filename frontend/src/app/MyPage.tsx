import { Link, useOutletContext } from 'react-router-dom';
import { clearAuthentication } from './authStore';
import { Button } from '../components/ui/Button';
import TopNavigation from '../components/ui/TopNavigation';
import type { Member } from '../types/Member';
import styles from './MyPage.module.css';

const MyPage = () => {
  const member = useOutletContext<Member>();
  return (
    <main className={styles.page}>
      <div className="page-container page-container--form">
        <TopNavigation title="마이페이지" />
        <h1 className="sr-only">마이페이지</h1>
        <section className="detail-section" aria-labelledby="member-heading">
          <div className={styles.memberHeading}>
            <h2 id="member-heading">회원 정보</h2>
            <Button variant="danger" className={styles.logoutButton} onClick={() => clearAuthentication('logout')}>
              로그아웃
            </Button>
          </div>
          <dl className={styles.memberSummary}>
            <div>
              <dt>이름</dt>
              <dd>{member.displayName}</dd>
            </div>
            <div>
              <dt>이메일</dt>
              <dd>{member.email}</dd>
            </div>
            <div>
              <dt>로그인</dt>
              <dd>Google 계정으로 로그인됨</dd>
            </div>
          </dl>
        </section>
        <section className="detail-section" aria-labelledby="upcoming-heading">
          <h2 id="upcoming-heading">준비 중인 기능</h2>
          <p className="section-note">1차 MVP에서는 안내 화면만 제공하며 실제 기능은 다음 범위에서 만나요.</p>
          <nav className={`${styles.links} ${styles.compactLinks}`} aria-label="준비 중인 기능">
            <Link to="/compare">
              <strong>비교표</strong>
              <span>여러 매물을 한눈에 비교해요.</span>
            </Link>
            <Link to="/export">
              <strong>내보내기</strong>
              <span>저장한 기록을 파일로 내려받아요.</span>
            </Link>
            <Link to="/tips">
              <strong>선배 팁</strong>
              <span>상황별 자취 경험을 확인해요.</span>
            </Link>
          </nav>
        </section>
      </div>
    </main>
  );
};

export default MyPage;
