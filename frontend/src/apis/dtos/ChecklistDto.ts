import type { ChecklistStage } from '../../types/Checklist';

export type ChecklistItemInputDto = { systemCheckItemId: number } | { systemCheckItemId: null; question: string };

export type CreateChecklistV11RequestDto = {
  name: string;
  stage: ChecklistStage;
  items: ChecklistItemInputDto[];
};

export type UpdateChecklistV11RequestDto = {
  name: string;
  items: ChecklistItemInputDto[];
};

/** @deprecated 최종 API 요청 이름으로 전환하기 전 화면 호환 타입이다. */
export type CreateChecklistRequestDto = {
  name: string;
  stage: ChecklistStage;
  checkItemIds: number[];
};

/** @deprecated 최종 API 요청 이름으로 전환하기 전 화면 호환 타입이다. */
export type UpdateChecklistRequestDto = {
  name: string;
  checkItemIds: number[];
};

export type AssignActiveChecklistRequestDto =
  { sourceType?: 'USER'; checklistId: number } | { sourceType: 'SYSTEM_DEFAULT'; checklistId: null };
