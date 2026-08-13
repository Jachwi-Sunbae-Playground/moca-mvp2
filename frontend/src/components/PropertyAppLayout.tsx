import { Link, NavLink, Outlet, useOutletContext } from 'react-router-dom';
import { clearAuthentication } from '../app/authStore';
import jachwiSunbaeLogo from '../assets/jachwi-sunbae-logo.png';
import type { Member } from '../types/Member';

const PropertyAppLayout = () => {
  const member = useOutletContext<Member>();

  return (
    <div className="property-app">
      <header className="app-header">
        <div className="app-header__inner">
          <Link className="app-header__brand" to="/properties" aria-label="자취선배 매물 목록">
            <img src={jachwiSunbaeLogo} alt="" />
            <span className="app-header__brand-name">자취선배</span>
          </Link>
          <div className="app-header__member">
            <Link to="/me" aria-label={`${member.displayName}님의 마이페이지`}>
              <span className="app-header__member-name">{member.displayName}님 · </span>마이
            </Link>
            <button type="button" className="text-button" onClick={() => clearAuthentication('logout')}>
              로그아웃
            </button>
          </div>
        </div>
      </header>
      <Outlet context={member} />
      <nav className="bottom-navigation" aria-label="주요 메뉴">
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
