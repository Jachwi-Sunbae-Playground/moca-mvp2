import type { RecentVisit } from '../types/Property';
import { formatDateTime, getVisitStatusLabel } from '../utils/propertyFormat';

type VisitSummaryPanelProps = {
  recentVisit: RecentVisit | null;
  compact?: boolean;
};

const VisitSummaryPanel = ({ recentVisit, compact = false }: VisitSummaryPanelProps) => {
  if (recentVisit === null) {
    return <p className="visit-empty">아직 방문 확인 기록이 없어요.</p>;
  }

  const { summary } = recentVisit;
  return (
    <div className={`visit-summary ${compact ? 'visit-summary--compact' : ''}`}>
      <div className="visit-summary__heading">
        <strong>{getVisitStatusLabel(recentVisit.status)}</strong>
        <span>{formatDateTime(recentVisit.startedAt)}</span>
      </div>
      <p>
        전체 {summary.totalCount}개 중 {summary.checkedCount}개 확인
      </p>
      <ul aria-label="최근 방문 결과 집계">
        <li>
          <span aria-hidden="true">●</span> 괜찮음 {summary.goodCount}
        </li>
        <li>
          <span aria-hidden="true">▲</span> 주의 {summary.cautionCount}
        </li>
        <li>
          <span aria-hidden="true">○</span> 미확인 {summary.unconfirmedCount}
        </li>
      </ul>
    </div>
  );
};

export default VisitSummaryPanel;
