import type { ChecklistStage } from '../../types/Checklist';

export type ProvidedChecklistItemRequestDto = {
  origin: 'PROVIDED';
  sourceCheckItemId: number;
  checklistItemId?: never;
  question?: never;
};

export type NewCustomChecklistItemRequestDto = {
  origin: 'CUSTOM';
  question: string;
  sourceCheckItemId?: never;
  checklistItemId?: never;
};

export type ExistingCustomChecklistItemRequestDto = {
  origin: 'CUSTOM';
  checklistItemId: number;
  question: string;
  sourceCheckItemId?: never;
};

export type CreateChecklistItemRequestDto = ProvidedChecklistItemRequestDto | NewCustomChecklistItemRequestDto;

export type UpdateChecklistItemRequestDto =
  ProvidedChecklistItemRequestDto | NewCustomChecklistItemRequestDto | ExistingCustomChecklistItemRequestDto;

export type CreateChecklistV11RequestDto = {
  name: string;
  stage: ChecklistStage;
  items: CreateChecklistItemRequestDto[];
  checkItemIds?: never;
};

export type UpdateChecklistV11RequestDto = {
  name: string;
  items: UpdateChecklistItemRequestDto[];
  checkItemIds?: never;
};

/** @deprecated v1.0 PROVIDED 전용 화면 호환 요청이다. */
export type CreateChecklistRequestDto = {
  name: string;
  stage: ChecklistStage;
  checkItemIds: number[];
  items?: never;
};

/** @deprecated v1.0 PROVIDED 전용 화면 호환 요청이다. */
export type UpdateChecklistRequestDto = {
  name: string;
  checkItemIds: number[];
  items?: never;
};

export type AssignActiveChecklistRequestDto = {
  checklistId: number;
};
