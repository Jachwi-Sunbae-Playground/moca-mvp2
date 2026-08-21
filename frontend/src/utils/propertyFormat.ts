import { checklistStageMeta } from '../constants/checklist';
import type { ChecklistStage } from '../types/Checklist';

export const formatWon = (amount: number): string => `${new Intl.NumberFormat('ko-KR').format(amount)}원`;

export const formatDateTime = (value: string): string =>
  new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));

export const getSafeHttpUrl = (value: string): string | null => {
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.toString() : null;
  } catch {
    return null;
  }
};

export const getChecklistStageLabel = (stage: ChecklistStage): string => checklistStageMeta[stage].label;

export const parsePositiveId = (value: string | undefined): number | null => {
  if (value === undefined || !/^\d+$/.test(value)) {
    return null;
  }

  const id = Number(value);
  return Number.isSafeInteger(id) && id > 0 ? id : null;
};
