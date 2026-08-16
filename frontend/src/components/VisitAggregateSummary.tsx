import type { VisitSummary } from '../types/Visit';
import styles from './VisitAggregateSummary.module.css';

const VisitAggregateSummary = ({ summary, label = '방문 결과 집계' }: { summary: VisitSummary; label?: string }) => (
  <div className={styles.aggregate} aria-label={label}>
    <p>
      전체 <strong>{summary.totalCount}</strong>개 중 <strong>{summary.checkedCount}</strong>개 확인
    </p>
    <ul>
      <li data-status="GOOD">● 괜찮음 {summary.goodCount}</li>
      <li data-status="CAUTION">▲ 주의 {summary.cautionCount}</li>
      <li data-status="UNCONFIRMED">○ 미확인 {summary.unconfirmedCount}</li>
    </ul>
  </div>
);

export default VisitAggregateSummary;
