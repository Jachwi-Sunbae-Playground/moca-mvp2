import { NavLink, Outlet, useLocation, useOutletContext } from 'react-router-dom';
import type { Member } from '../types/Member';
import Icon from './ui/Icon';
import styles from './PropertyAppLayout.module.css';

const PropertyAppLayout = () => {
  const member = useOutletContext<Member>();
  const location = useLocation();
  const fullScreen = location.pathname === '/map/select-location';

  return (
    <div className={styles.root} data-full-screen={fullScreen || undefined}>
      <div className={styles.content}>
        <Outlet context={member} />
      </div>
      {!fullScreen && (
        <nav className={styles.bottomNavigation} aria-label="주요 메뉴">
          <NavLink to="/properties" aria-label="홈">
            <Icon name="home" size={16} />홈
          </NavLink>
          <NavLink to="/checklists" aria-label="체크리스트">
            <Icon name="checklist" size={16} />
            체크리스트
          </NavLink>
          <NavLink to="/map" aria-label="지도">
            <Icon name="map" size={16} />
            지도
          </NavLink>
          <NavLink to="/me" aria-label="마이">
            <Icon name="user" size={16} />
            마이
          </NavLink>
        </nav>
      )}
    </div>
  );
};

export default PropertyAppLayout;
