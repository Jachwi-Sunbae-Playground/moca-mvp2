import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import Icon from './Icon';
import styles from './AppBar.module.css';

type AppBarProps = {
  title: string;
  backTo?: string;
  backLabel?: string;
  action?: ReactNode;
};

const AppBar = ({ title, backTo, backLabel = '뒤로 가기', action }: AppBarProps) => (
  <header className={styles.appBar}>
    <div className={styles.side}>
      {backTo !== undefined && (
        <Link className={styles.iconLink} to={backTo} aria-label={backLabel}>
          <Icon name="arrow-left" size={22} />
        </Link>
      )}
    </div>
    <strong className={styles.title}>{title}</strong>
    <div className={`${styles.side} ${styles.action}`}>{action}</div>
  </header>
);

export default AppBar;
