import type { ReactNode } from 'react';
import Icon from './Icon';
import styles from './TopNavigationMenu.module.css';

type TopNavigationMenuProps = {
  children: ReactNode;
  label?: string;
};

const TopNavigationMenu = ({ children, label = '페이지 메뉴 열기' }: TopNavigationMenuProps) => (
  <details className={styles.menu}>
    <summary aria-label={label}>
      <Icon name="more-vertical" size={22} />
    </summary>
    <div className={styles.items}>{children}</div>
  </details>
);

export default TopNavigationMenu;
