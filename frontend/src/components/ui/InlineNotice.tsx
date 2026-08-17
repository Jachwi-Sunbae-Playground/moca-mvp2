import type { ReactNode } from 'react';
import Icon from './Icon';
import styles from './InlineNotice.module.css';

type InlineNoticeTone = 'info' | 'warning' | 'error';

type InlineNoticeProps = {
  children: ReactNode;
  tone?: InlineNoticeTone;
};

const InlineNotice = ({ children, tone = 'info' }: InlineNoticeProps) => (
  <div className={`${styles.notice} ${styles[tone]}`} role={tone === 'error' ? 'alert' : undefined}>
    <Icon name="info" size={17} />
    <span>{children}</span>
  </div>
);

export default InlineNotice;
