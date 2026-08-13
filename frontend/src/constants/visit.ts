import type { VisitItemStatus, VisitStatus } from '../types/Visit';

export const VISIT_ITEM_STATUSES = ['GOOD', 'CAUTION', 'UNCONFIRMED'] as const satisfies readonly VisitItemStatus[];

export const visitItemStatusMeta: Record<VisitItemStatus, { label: string; symbol: string; description: string }> = {
  GOOD: { label: '괜찮음', symbol: '●', description: '문제가 없다고 확인함' },
  CAUTION: { label: '주의', symbol: '▲', description: '추가 확인이나 협의가 필요함' },
  UNCONFIRMED: { label: '미확인', symbol: '○', description: '아직 확인하지 못함' },
};

export const visitStatusLabel: Record<VisitStatus, string> = {
  IN_PROGRESS: '확인 진행 중',
  COMPLETED: '방문 완료',
};

export const isVisitStatus = (value: string): value is VisitStatus => value === 'IN_PROGRESS' || value === 'COMPLETED';

export const isVisitItemStatus = (value: string): value is VisitItemStatus =>
  value === 'GOOD' || value === 'CAUTION' || value === 'UNCONFIRMED';
