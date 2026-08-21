import type { CheckItem, ChecklistStage } from '../types/Checklist';
import { isChecklistStage } from '../constants/checklist';

export const validateChecklistName = (value: string): string | null => {
  const name = value.trim();
  if (name.length === 0) return '체크리스트 이름을 입력해 주세요.';
  if (name.length > 50) return '체크리스트 이름은 50자 이하로 입력해 주세요.';
  return null;
};

export const hasUniqueSameStageItems = (items: CheckItem[], stage: ChecklistStage): boolean =>
  items.length > 0 &&
  items.every((item) => item.stage === stage) &&
  new Set(items.map((item) => item.checkItemId)).size === items.length;

export const moveChecklistItem = (items: CheckItem[], index: number, direction: -1 | 1): CheckItem[] => {
  const destination = index + direction;
  if (index < 0 || index >= items.length || destination < 0 || destination >= items.length) return items;
  const result = [...items];
  [result[index], result[destination]] = [result[destination], result[index]];
  return result;
};

export type ChecklistReturnTarget = { propertyId: number; stage: ChecklistStage; path: string };

export const parseChecklistReturnTo = (value: string | null): ChecklistReturnTarget | null => {
  if (value === null) return null;
  const match =
    /^\/properties\/(\d+)\/active-checklists\/(ONLINE_PHONE|ON_SITE|PRE_CONTRACT)(?:\?from=property-detail)?$/.exec(
      value,
    );
  if (match === null || !isChecklistStage(match[2])) return null;
  const propertyId = Number(match[1]);
  if (!Number.isSafeInteger(propertyId) || propertyId < 1) return null;
  return { propertyId, stage: match[2], path: value };
};
