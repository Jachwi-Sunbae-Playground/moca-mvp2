import { Link, NavLink, Outlet, useOutletContext } from 'react-router-dom';
import { clearAuthentication } from '../app/authStore';
import jachwiSunbaeLogo from '../assets/jachwi-sunbae-logo.png';
import type { Member } from '../types/Member';
import styles from './PropertyAppLayout.module.css';

const PropertyAppLayout = () => {
  const member = useOutletContext<Member>();

  return (
    <div className={styles.root}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link className={styles.brand} to="/properties" aria-label="자취선배 매물 목록">
            <img src={jachwiSunbaeLogo} alt="" />
            <span className={styles.brandName}>자취선배</span>
          </Link>
          <div className={styles.member}>
            <Link to="/me" aria-label={`${member.displayName}님의 마이페이지`}>
              <span className={styles.memberName}>{member.displayName}님 · </span>마이
            </Link>
            <button type="button" className={styles.logoutButton} onClick={() => clearAuthentication('logout')}>
              로그아웃
            </button>
          </div>
        </div>
      </header>
      <Outlet context={member} />
      <nav className={styles.bottomNavigation} aria-label="주요 메뉴">
        <NavLink to="/properties" aria-label="홈">
          <span aria-hidden="true">⌂</span>홈
        </NavLink>
        <NavLink to="/checklists" aria-label="체크리스트">
          <span aria-hidden="true">✓</span>
          체크리스트
        </NavLink>
      </nav>
    </div>
  );
};

export default PropertyAppLayout;
