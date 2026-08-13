import type {
  DeletionImpact,
  DiscoverySource,
  PropertyActiveChecklist,
  PropertyBasicInfo,
  PropertyDetail,
  PropertyMemo,
  PropertyPage,
  PropertyPhoto,
  PropertyPhotoList,
  PropertyPhotoPreview,
  PropertySummary,
  RecentVisit,
} from '../types/Property';
import { isVisitStatus } from '../constants/visit';
import type { VisitSummary } from '../types/Visit';
import {
  readArray,
  readBoolean,
  readInteger,
  readNullableUtcDateTime,
  readRecord,
  readString,
  readUtcDateTime,
} from './responseParsers';

const parseDiscoverySource = (value: unknown): DiscoverySource => {
  const record = readRecord(value);
  const type = readString(record, 'type');

  if (type !== 'URL' && type !== 'TEXT') {
    throw new Error('발견 경로 유형이 올바르지 않습니다.');
  }

  return { type, value: readString(record, 'value') };
};

const parseVisitSummary = (value: unknown): VisitSummary => {
  const record = readRecord(value);
  return {
    totalCount: readInteger(record, 'totalCount'),
    checkedCount: readInteger(record, 'checkedCount'),
    goodCount: readInteger(record, 'goodCount'),
    cautionCount: readInteger(record, 'cautionCount'),
    unconfirmedCount: readInteger(record, 'unconfirmedCount'),
  };
};

const parseRecentVisit = (value: unknown): RecentVisit | null => {
  if (value === null) {
    return null;
  }

  const record = readRecord(value);
  const status = readString(record, 'status');

  if (!isVisitStatus(status)) {
    throw new Error('방문 상태 응답이 올바르지 않습니다.');
  }

  return {
    visitId: readInteger(record, 'visitId', 1),
    status,
    startedAt: readUtcDateTime(record, 'startedAt'),
    completedAt: readNullableUtcDateTime(record, 'completedAt'),
    summary: parseVisitSummary(record.summary),
  };
};

const parsePropertySummary = (value: unknown): PropertySummary => {
  const record = readRecord(value);
  return {
    propertyId: readInteger(record, 'propertyId', 1),
    name: readString(record, 'name'),
    depositAmount: readInteger(record, 'depositAmount'),
    monthlyRentAmount: readInteger(record, 'monthlyRentAmount'),
    discoverySource: parseDiscoverySource(record.discoverySource),
    recentVisit: parseRecentVisit(record.recentVisit),
    photoCount: readInteger(record, 'photoCount'),
    lastActivityAt: readUtcDateTime(record, 'lastActivityAt'),
  };
};

const parsePropertyActiveChecklist = (value: unknown): PropertyActiveChecklist => {
  const record = readRecord(value);
  const stage = readString(record, 'stage');

  if (stage !== 'ONLINE_PHONE' && stage !== 'ON_SITE' && stage !== 'PRE_CONTRACT') {
    throw new Error('체크리스트 단계가 올바르지 않습니다.');
  }

  return {
    stage,
    checklistId: readInteger(record, 'checklistId', 1),
    name: readString(record, 'name'),
    itemCount: readInteger(record, 'itemCount'),
  };
};

const parsePhotoPreview = (value: unknown): PropertyDetail['photoPreview'] => {
  const record = readRecord(value);

  return {
    totalCount: readInteger(record, 'totalCount'),
    photos: readArray(record, 'photos').map((photo): PropertyPhotoPreview => {
      const photoRecord = readRecord(photo);
      return {
        photoId: readInteger(photoRecord, 'photoId', 1),
        contentUrl: readString(photoRecord, 'contentUrl'),
        createdAt: readUtcDateTime(photoRecord, 'createdAt'),
      };
    }),
  };
};

const parseDeletionImpact = (value: unknown): DeletionImpact => {
  const record = readRecord(value);
  return {
    visitCount: readInteger(record, 'visitCount'),
    photoCount: readInteger(record, 'photoCount'),
    activeChecklistCount: readInteger(record, 'activeChecklistCount'),
  };
};

const parsePropertyMemo = (value: unknown): PropertyMemo => {
  const record = readRecord(value);
  const additionalMemo = readString(record, 'additionalMemo', { allowEmpty: true, maximumCodePoints: 5_000 });
  const content = readString(record, 'content', { allowEmpty: true, maximumCodePoints: 5_000 });
  if (content !== additionalMemo) throw new Error('content와 additionalMemo가 일치하지 않습니다.');

  return {
    viewingSchedule: readString(record, 'viewingSchedule', { allowEmpty: true, maximumCodePoints: 200 }),
    moveInAvailability: readString(record, 'moveInAvailability', { allowEmpty: true, maximumCodePoints: 200 }),
    provisionalDeposit: readString(record, 'provisionalDeposit', { allowEmpty: true, maximumCodePoints: 200 }),
    roomOptions: readString(record, 'roomOptions', { allowEmpty: true, maximumCodePoints: 200 }),
    maintenanceAndUtilities: readString(record, 'maintenanceAndUtilities', {
      allowEmpty: true,
      maximumCodePoints: 200,
    }),
    commuteTime: readString(record, 'commuteTime', { allowEmpty: true, maximumCodePoints: 200 }),
    governmentSupport: readString(record, 'governmentSupport', { allowEmpty: true, maximumCodePoints: 200 }),
    additionalMemo,
    content,
    savedAt: readNullableUtcDateTime(record, 'savedAt'),
  };
};

export const parsePropertyPage = (value: unknown): PropertyPage => {
  const record = readRecord(value);

  return {
    content: readArray(record, 'content').map(parsePropertySummary),
    page: readInteger(record, 'page'),
    size: readInteger(record, 'size', 1),
    totalElements: readInteger(record, 'totalElements'),
    totalPages: readInteger(record, 'totalPages'),
    hasNext: readBoolean(record, 'hasNext'),
  };
};

export const parsePropertyDetail = (value: unknown): PropertyDetail => {
  const record = readRecord(value);

  return {
    propertyId: readInteger(record, 'propertyId', 1),
    name: readString(record, 'name'),
    depositAmount: readInteger(record, 'depositAmount'),
    monthlyRentAmount: readInteger(record, 'monthlyRentAmount'),
    discoverySource: parseDiscoverySource(record.discoverySource),
    memo: parsePropertyMemo(record.memo),
    activeChecklists: readArray(record, 'activeChecklists').map(parsePropertyActiveChecklist),
    recentVisit: parseRecentVisit(record.recentVisit),
    photoPreview: parsePhotoPreview(record.photoPreview),
    deletionImpact: parseDeletionImpact(record.deletionImpact),
    createdAt: readUtcDateTime(record, 'createdAt'),
    updatedAt: readUtcDateTime(record, 'updatedAt'),
    lastActivityAt: readUtcDateTime(record, 'lastActivityAt'),
  };
};

export const parsePropertyBasicInfo = (value: unknown): PropertyBasicInfo => {
  const record = readRecord(value);
  return {
    propertyId: readInteger(record, 'propertyId', 1),
    name: readString(record, 'name'),
    depositAmount: readInteger(record, 'depositAmount'),
    monthlyRentAmount: readInteger(record, 'monthlyRentAmount'),
    discoverySource: parseDiscoverySource(record.discoverySource),
    updatedAt:
      typeof record.updatedAt === 'string'
        ? readUtcDateTime(record, 'updatedAt')
        : readUtcDateTime(record, 'createdAt'),
  };
};

export const parsePropertyMemoResponse = parsePropertyMemo;

const parsePropertyPhoto = (value: unknown): PropertyPhoto => {
  const record = readRecord(value);
  const contentType = readString(record, 'contentType');

  if (contentType !== 'image/jpeg' && contentType !== 'image/png' && contentType !== 'image/webp') {
    throw new Error('사진 형식 응답이 올바르지 않습니다.');
  }

  return {
    photoId: readInteger(record, 'photoId', 1),
    contentUrl: readString(record, 'contentUrl'),
    contentType,
    sizeBytes: readInteger(record, 'sizeBytes', 1),
    createdAt: readUtcDateTime(record, 'createdAt'),
  };
};

export const parsePropertyPhotoResponse = parsePropertyPhoto;

export const parsePropertyPhotoList = (value: unknown): PropertyPhotoList => {
  const record = readRecord(value);

  return { photos: readArray(record, 'photos').map(parsePropertyPhoto), totalCount: readInteger(record, 'totalCount') };
};

export const parseNoContent = (value: unknown): undefined => {
  if (value !== undefined) {
    throw new Error('본문 없는 응답이 필요합니다.');
  }

  return undefined;
};
