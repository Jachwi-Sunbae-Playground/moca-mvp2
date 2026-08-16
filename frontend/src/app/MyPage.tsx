import { Link, useOutletContext } from 'react-router-dom';
import { clearAuthentication } from './authStore';
import type { Member } from '../types/Member';
import styles from './MyPage.module.css';

const MyPage = () => {
  const member = useOutletContext<Member>();
  return (
    <main className={`property-page ${styles.page}`}>
      <div className="page-container page-container--form">
        <p className="section-eyebrow">내 정보</p>
        <h1>마이페이지</h1>
        <section className="detail-section" aria-labelledby="member-heading">
          <h2 id="member-heading">회원 정보</h2>
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
        <nav className={styles.links} aria-label="내 기록 바로가기">
          <Link to="/properties">
            <strong>내 매물</strong>
            <span>매물과 방문 기록을 확인해요.</span>
          </Link>
          <Link to="/checklists">
            <strong>내 체크리스트</strong>
            <span>단계별 확인 질문을 관리해요.</span>
          </Link>
        </nav>
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
        <section className="detail-section" aria-labelledby="session-heading">
          <h2 id="session-heading">현재 로그인</h2>
          <p className="section-note">
            보안을 위해 인증 정보는 이 탭의 메모리에만 있으며 새로고침하면 다시 로그인해야 합니다.
          </p>
          <button className="danger-outline-button" type="button" onClick={() => clearAuthentication('logout')}>
            로그아웃
          </button>
        </section>
      </div>
    </main>
  );
};

export default MyPage;
