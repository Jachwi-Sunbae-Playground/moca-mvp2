import { NavLink, Outlet, useOutletContext } from 'react-router-dom';
import type { Member } from '../types/Member';
import Icon from './ui/Icon';
import styles from './PropertyAppLayout.module.css';

const PropertyAppLayout = () => {
  const member = useOutletContext<Member>();

  return (
    <div className={styles.root}>
      <Outlet context={member} />
      <nav className={styles.bottomNavigation} aria-label="주요 메뉴">
        <NavLink to="/properties" aria-label="홈">
          <Icon name="home" size={21} />홈
        </NavLink>
        <NavLink to="/checklists" aria-label="체크리스트">
          <Icon name="checklist" size={21} />
          체크리스트
        </NavLink>
        <NavLink to="/me" aria-label="마이">
          <Icon name="user" size={21} />
          마이
        </NavLink>
      </nav>
    </div>
  );
};

export default PropertyAppLayout;
