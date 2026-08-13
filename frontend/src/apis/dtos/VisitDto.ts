import type { VisitItemStatus } from '../../types/Visit';

export type UpdateVisitItemStatusRequestDto = {
  status: VisitItemStatus;
  expectedStatusVersion: number;
  expectedVersion?: never;
  memo?: never;
  expectedMemoVersion?: never;
};

export type UpdateVisitItemMemoRequestDto = {
  memo: string;
  expectedMemoVersion: number;
  status?: never;
  expectedStatusVersion?: never;
  expectedVersion?: never;
};

/** @deprecated v1.0 상태 CAS 요청 호환 전용이다. */
export type UpdateVisitItemRequestDto = {
  status: VisitItemStatus;
  expectedVersion: number;
  expectedStatusVersion?: never;
  memo?: never;
  expectedMemoVersion?: never;
};

export type CompleteVisitRequestDto = {
  status: 'COMPLETED';
};
