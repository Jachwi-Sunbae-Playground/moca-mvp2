import { ApiError, getSafeApiErrorMessage } from './apiClient';

const messages: Record<string, string> = {
  INVALID_REQUEST: '입력한 내용을 다시 확인해 주세요.',
  INVALID_PAGE_REQUEST: '목록 페이지 요청이 올바르지 않습니다.',
  PROPERTY_NOT_FOUND: '매물을 찾을 수 없습니다. 목록에서 다시 확인해 주세요.',
  PROPERTY_MEMO_INVALID: '메모 입력 길이와 필수 항목을 다시 확인해 주세요.',
  AMBIGUOUS_MEMO_CONTENT: '메모 저장 형식이 서로 충돌합니다. 화면을 새로 연 뒤 다시 시도해 주세요.',
  PHOTO_NOT_FOUND: '사진을 찾을 수 없습니다. 사진 목록을 새로 확인해 주세요.',
  PHOTO_FORMAT_UNSUPPORTED: 'JPEG, PNG 또는 WebP 사진만 등록할 수 있습니다.',
  PHOTO_SIZE_EXCEEDED: '사진 한 장은 10MiB 이하만 등록할 수 있습니다.',
  PHOTO_COUNT_EXCEEDED: '매물마다 사진을 최대 30장까지 등록할 수 있습니다.',
  PHOTO_UPLOAD_FAILED: '사진을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.',
  PHOTO_READ_FAILED: '사진을 불러오지 못했습니다. 다시 시도해 주세요.',
  PHOTO_DELETE_FAILED: '사진을 삭제하지 못했습니다. 기존 사진은 그대로 유지됩니다.',
};

export const getPropertyErrorMessage = (error: unknown): string => {
  if (error instanceof ApiError && error.code !== null && messages[error.code] !== undefined) {
    return messages[error.code];
  }

  return getSafeApiErrorMessage(error);
};
