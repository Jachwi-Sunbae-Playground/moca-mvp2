import { ApiError, getSafeApiErrorMessage } from './apiClient';

const messages: Record<string, string> = {
  INVALID_STAGE: '확인 단계를 다시 선택해 주세요.',
  INVALID_REQUEST: '입력한 이름과 체크 항목을 확인해 주세요.',
  INVALID_PAGE_REQUEST: '목록 요청이 올바르지 않아요. 화면을 다시 열어 주세요.',
  CHECKLIST_PRESET_NOT_FOUND: '선택한 프리셋을 불러올 수 없어요. 다른 프리셋을 선택해 주세요.',
  CHECKLIST_NOT_FOUND: '체크리스트를 찾을 수 없어요.',
  CHECKLIST_EMPTY: '체크 항목을 한 개 이상 선택해 주세요.',
  CHECKLIST_ITEM_DUPLICATED: '같은 체크 항목을 두 번 담을 수 없어요.',
  CHECKLIST_ITEM_STAGE_MISMATCH: '현재 단계에 맞는 체크 항목만 선택해 주세요.',
  CUSTOM_CHECKLIST_ITEM_INVALID: '직접 추가한 질문과 항목 구성을 다시 확인해 주세요.',
  CHECKLIST_ITEM_NOT_FOUND: '체크리스트의 로컬 항목을 찾을 수 없어요. 화면을 다시 열어 주세요.',
  CHECKLIST_ITEMS_REPRESENTATION_CONFLICT: '체크리스트 저장 형식이 서로 충돌해요. 화면을 다시 열어 주세요.',
  CHECKLIST_REQUIRES_V11_CLIENT: '직접 추가한 항목을 보호하려면 최신 편집 화면에서 다시 시도해 주세요.',
  CHECK_ITEM_INACTIVE: '더 이상 제공되지 않는 항목이 있어요. 해당 항목을 제거한 뒤 다시 저장해 주세요.',
  CHECK_ITEM_NOT_FOUND: '찾을 수 없는 체크 항목이 있어요. 목록을 다시 확인해 주세요.',
  CHECKLIST_STAGE_MISMATCH: '현재 매물 확인 단계와 같은 체크리스트를 선택해 주세요.',
  PROPERTY_NOT_FOUND: '매물을 찾을 수 없어요.',
};

export const getChecklistErrorMessage = (error: unknown): string => {
  if (error instanceof ApiError && error.code !== null && error.code in messages) return messages[error.code];
  return getSafeApiErrorMessage(error);
};
