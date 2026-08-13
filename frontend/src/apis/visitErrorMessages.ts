import { ApiError, getSafeApiErrorMessage } from './apiClient';

const visitErrorMessages: Record<string, string> = {
  PROPERTY_NOT_FOUND: '매물을 찾을 수 없어요.',
  ACTIVE_CHECKLIST_REQUIRED: '연결된 활성 체크리스트가 없어 방문을 시작할 수 없어요.',
  CHECKLIST_SNAPSHOT_FAILED: '체크리스트를 복사하지 못했어요. 방문은 만들어지지 않았습니다.',
  VISIT_NOT_FOUND: '방문 기록을 찾을 수 없어요.',
  VISIT_ITEM_NOT_FOUND: '이 방문 항목을 찾을 수 없어요.',
  INVALID_CHECK_STATUS: '선택한 확인 상태를 저장할 수 없어요.',
  AMBIGUOUS_STATUS_VERSION: '상태 저장 기준이 서로 충돌해요. 방문 기록을 다시 불러와 주세요.',
  VISIT_ITEM_STATUS_VERSION_CONFLICT: '다른 상태 변경이 먼저 저장됐어요. 최신 기록을 확인해 주세요.',
  VISIT_ITEM_MEMO_VERSION_CONFLICT: '다른 메모 변경이 먼저 저장됐어요. 최신 기록을 확인해 주세요.',
  VISIT_ITEM_MEMO_INVALID: '인라인 메모는 줄바꿈 없이 200자 이내로 입력해 주세요.',
  INVALID_VISIT_STATUS: '요청한 방문 상태로 변경할 수 없어요.',
  INVALID_PAGE_REQUEST: '방문 목록 범위를 확인해 주세요.',
};

export const getVisitErrorMessage = (error: unknown): string => {
  if (!(error instanceof ApiError) || error.code === null) return getSafeApiErrorMessage(error);
  return visitErrorMessages[error.code] ?? getSafeApiErrorMessage(error);
};

export const isVisitStatusVersionConflict = (error: unknown): boolean =>
  error instanceof ApiError &&
  (error.code === 'VISIT_ITEM_STATUS_VERSION_CONFLICT' || error.code === 'VISIT_ITEM_VERSION_MISMATCH');

export const isVisitMemoVersionConflict = (error: unknown): boolean =>
  error instanceof ApiError && error.code === 'VISIT_ITEM_MEMO_VERSION_CONFLICT';

/** @deprecated 상태 채널 호환 이름이다. */
export const isVisitVersionMismatch = isVisitStatusVersionConflict;

export const isAmbiguousVisitNetworkError = (error: unknown): boolean =>
  error instanceof ApiError && error.kind === 'network';
