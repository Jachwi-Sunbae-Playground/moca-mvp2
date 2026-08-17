import type { ReactNode } from 'react';
import Icon from './Icon';
import type { IconName } from './Icon';
import styles from './EmptyState.module.css';

type EmptyStateProps = {
  title: string;
  description: string;
  action?: ReactNode;
  icon?: IconName;
};

const EmptyState = ({ title, description, action, icon = 'inbox' }: EmptyStateProps) => (
  <section className={styles.emptyState}>
    <span className={styles.icon} aria-hidden="true">
      <Icon name={icon} size={26} />
    </span>
    <strong>{title}</strong>
    <p>{description}</p>
    {action !== undefined && <div className={styles.action}>{action}</div>}
  </section>
);

export default EmptyState;
