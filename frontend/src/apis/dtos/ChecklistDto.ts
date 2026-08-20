import type { ChecklistStage } from '../../types/Checklist';

export type CreateChecklistV11RequestDto = {
  name: string;
  stage: ChecklistStage;
  optionalSystemCheckItemIds: number[];
};

export type UpdateChecklistV11RequestDto = {
  name: string;
  systemCheckItemIds: number[];
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

export type AssignActiveChecklistRequestDto = {
  checklistId: number;
};
