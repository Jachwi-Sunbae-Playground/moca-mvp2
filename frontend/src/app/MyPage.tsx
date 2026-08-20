import { Link, useOutletContext } from 'react-router-dom';
import { clearAuthentication } from './authStore';
import { Button } from '../components/ui/Button';
import Icon from '../components/ui/Icon';
import TopNavigation from '../components/ui/TopNavigation';
import type { Member } from '../types/Member';
import styles from './MyPage.module.css';

const MyPage = () => {
  const member = useOutletContext<Member>();
  const displayInitial = member.displayName.trim().slice(0, 1) || '자';

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <TopNavigation title="마이" backTo="/properties" backLabel="홈으로 돌아가기" />
        <section className={styles.profileCard} aria-labelledby="member-heading">
          <span className={styles.avatar} aria-hidden="true">
            {displayInitial}
          </span>
          <div className={styles.memberInfo}>
            <h2 id="member-heading">{member.displayName}</h2>
            <p>{member.email}</p>
            <small>Google 계정 연결됨</small>
          </div>
        </section>
        <nav className={styles.menu} aria-label="내 기록">
          <Link to="/properties">
            <span className={styles.menuIcon}>
              <Icon name="home" size={15} />
            </span>
            <strong>내 매물 관리</strong>
            <Icon name="arrow-right" size={15} />
          </Link>
          <Link to="/checklists">
            <span className={styles.menuIcon}>
              <Icon name="checklist" size={15} />
            </span>
            <strong>내 체크리스트 관리</strong>
            <Icon name="arrow-right" size={15} />
          </Link>
          <Link to="/export">
            <span className={styles.menuIcon}>
              <Icon name="external-link" size={15} />
            </span>
            <strong>내보낸 비교표</strong>
            <Icon name="arrow-right" size={15} />
          </Link>
        </nav>
        <Link className={styles.notice} to="/tips">
          <span className={styles.noticeIcon}>
            <Icon name="info" size={16} />
          </span>
          <span>
            <strong>선배팁 · 계약 전 꼭 확인할 7가지</strong>
            <small>먼저 자취한 선배들이 남긴 정보를 확인해요.</small>
          </span>
          <Icon name="arrow-right" size={15} />
        </Link>
        <footer className={styles.footer}>
          <span>자취선배 v0.1.0</span>
          <Button variant="text" className={styles.logoutButton} onClick={() => clearAuthentication('logout')}>
            로그아웃
          </Button>
        </footer>
      </div>
    </main>
  );
};

export default MyPage;
