import type { ReactNode } from 'react';
import errorIllustration from '../assets/jachwi-sunbae-error.png';
import styles from './StatusPanel.module.css';

type StatusPanelProps = {
  title: string;
  description: string;
  tone?: 'neutral' | 'error' | 'success';
  isBusy?: boolean;
  action?: ReactNode;
};

const toneClassNames: Record<NonNullable<StatusPanelProps['tone']>, string> = {
  neutral: '',
  error: styles.error,
  success: styles.success,
};

const StatusPanel = ({ title, description, tone = 'neutral', isBusy = false, action }: StatusPanelProps) => (
  <main className={styles.page}>
    <section
      className={`${styles.panel} ${toneClassNames[tone]}`}
      aria-live={isBusy ? 'polite' : undefined}
      aria-busy={isBusy || undefined}
    >
      {tone === 'error' && !isBusy ? (
        <div className={styles.errorIllustrationViewport} aria-hidden="true">
          <img className={styles.errorIllustration} src={errorIllustration} alt="" />
        </div>
      ) : (
        <div className={styles.mark} aria-hidden="true">
          {isBusy ? <span className={styles.spinner} /> : tone === 'success' ? '✓' : '·'}
        </div>
      )}
      <h1>{title}</h1>
      <p>{description}</p>
      {action === undefined ? null : <div className={styles.action}>{action}</div>}
    </section>
  </main>
);

export default StatusPanel;
