import type {
  DiscoverySource,
  PropertyBasicInfo,
  PropertyChecklistOverview,
  PropertyChecklistProgress,
  PropertyChecklistStageSummary,
  PropertyChecklistDetail,
  PropertyChecklistItemStatus,
  PropertyDetail,
  PropertyMemoDocument,
  PropertyPage,
  PropertyPhoto,
  PropertyPhotoList,
  PropertyPhotoPreview,
  PropertySummary,
  PropertyLocation,
} from '../types/Property';
import {
  readArray,
  readBoolean,
  readInteger,
  readNullableInteger,
  readNullableString,
  readRecord,
  readString,
  readUtcDateTime,
} from './responseParsers';

const parseDiscoverySource = (value: unknown): DiscoverySource => {
  if (value === null) return { type: 'TEXT', value: '' };
  if (typeof value === 'string') {
    return { type: /^https?:\/\//i.test(value) ? 'URL' : 'TEXT', value };
  }
  if (value === undefined || typeof value !== 'object' || Array.isArray(value)) {
    return { type: 'TEXT', value: '' };
  }
  const record = readRecord(value);
  const type = record.type;

  if (type === 'URL' || type === 'TEXT') {
    const sourceValue = typeof record.value === 'string' ? record.value : '';
    return { type, value: sourceValue };
  }

  return { type: 'TEXT', value: '' };
};

const readDetailInteger = (record: Record<string, unknown>, key: string): number => {
  const value = record[key];
  return typeof value === 'number' && Number.isSafeInteger(value) ? value : 0;
};

const readDetailString = (record: Record<string, unknown>, key: string, fallback: string): string => {
  const value = record[key];
  return typeof value === 'string' ? value : fallback;
};

const parsePropertyLocation = (record: Record<string, unknown>): PropertyLocation => {
  const nullableText = (key: string): string | null => {
    const value = record[key];
    return typeof value === 'string' && value.trim() !== '' ? value : null;
  };
  const nullableCoordinate = (key: string): number | null => {
    const value = record[key];
    return typeof value === 'number' && Number.isFinite(value) ? value : null;
  };
  return {
    address: nullableText('address'),
    roadAddress: nullableText('roadAddress'),
    jibunAddress: nullableText('jibunAddress'),
    latitude: nullableCoordinate('latitude'),
    longitude: nullableCoordinate('longitude'),
  };
};

const readOptionalUtcDateTime = (record: Record<string, unknown>, key: string): string =>
  typeof record[key] === 'string' ? readUtcDateTime(record, key) : '1970-01-01T00:00:00Z';

const parseDetailPhoto = (value: unknown): PropertyPhotoPreview | null => {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) return null;
  const record = value as Record<string, unknown>;
  const photoId = readDetailInteger(record, 'id') || readDetailInteger(record, 'photoId');
  const contentUrl = readDetailString(record, 'url', readDetailString(record, 'contentUrl', ''));
  if (photoId < 1 || contentUrl === '') return null;

  const rawCreatedAt = record.createdAt ?? record.created_at;
  const createdAt =
    typeof rawCreatedAt === 'string'
      ? (() => {
          try {
            return readUtcDateTime({ createdAt: rawCreatedAt }, 'createdAt');
          } catch {
            return '1970-01-01T00:00:00Z';
          }
        })()
      : '1970-01-01T00:00:00Z';

  return { photoId, contentUrl, createdAt };
};

const parsePropertySummary = (value: unknown): PropertySummary => {
  const record = readRecord(value);
  const representativePhoto = record.representativePhoto;
  const parsedRepresentativePhoto =
    representativePhoto === null || representativePhoto === undefined
      ? null
      : (() => {
          const photo = readRecord(representativePhoto);
          const rawContentType = readString(photo, 'contentType');
          if (rawContentType !== 'image/jpeg' && rawContentType !== 'image/png' && rawContentType !== 'image/webp') {
            throw new Error('대표 사진 형식 응답이 올바르지 않습니다.');
          }
          const contentType: 'image/jpeg' | 'image/png' | 'image/webp' = rawContentType;
          return {
            photoId: readInteger(photo, 'id', 1),
            contentUrl: readString(photo, 'url'),
            contentType,
          };
        })();

  return {
    propertyId: readInteger(record, 'id', 1),
    name: readString(record, 'name'),
    depositAmount: readInteger(record, 'depositAmount'),
    monthlyRentAmount: readInteger(record, 'monthlyRentAmount'),
    discoverySource: parseDiscoverySource(record.discoverySource),
    location: parsePropertyLocation(record),
    representativePhoto: parsedRepresentativePhoto,
    progress: parsePropertyChecklistProgress(record.overallProgress),
    stages: Array.isArray(record.stages) ? record.stages.map(parsePropertyChecklistStageSummary) : [],
    photoCount: typeof record.photoCount === 'number' ? readInteger(record, 'photoCount') : 0,
    lastActivityAt: readOptionalUtcDateTime(record, 'lastActivityAt'),
  };
};

export const parsePropertyPage = (value: unknown): PropertyPage => {
  const record = readRecord(value);
  const content = readArray(record, 'items').map(parsePropertySummary);
  const totalElements = readInteger(record, 'totalCount');
  if (content.length !== totalElements) throw new Error('매물 목록 집계 응답이 올바르지 않습니다.');

  return {
    content,
    page: 0,
    size: Math.max(1, content.length),
    totalElements,
    totalPages: totalElements === 0 ? 0 : 1,
    hasNext: false,
  };
};

export const parsePropertyDetail = (value: unknown): PropertyDetail => {
  const record = readRecord(value);
  const photos = Array.isArray(record.photos)
    ? record.photos.flatMap((photo) => {
        const parsedPhoto = parseDetailPhoto(photo);
        return parsedPhoto === null ? [] : [parsedPhoto];
      })
    : [];

  return {
    propertyId: readDetailInteger(record, 'propertyId') || readDetailInteger(record, 'id'),
    name: readDetailString(record, 'name', '이름 없는 매물'),
    depositAmount: readDetailInteger(record, 'depositAmount'),
    monthlyRentAmount: readDetailInteger(record, 'monthlyRentAmount'),
    discoverySource: parseDiscoverySource(record.discoverySource),
    location: parsePropertyLocation(record),
    photoPreview: {
      totalCount: typeof record.photoCount === 'number' ? readInteger(record, 'photoCount') : photos.length,
      photos,
    },
    createdAt: readOptionalUtcDateTime(record, 'createdAt'),
    updatedAt: readOptionalUtcDateTime(record, 'updatedAt'),
    lastActivityAt: readOptionalUtcDateTime(record, 'lastActivityAt'),
  };
};

export const parsePropertyBasicInfo = (value: unknown): PropertyBasicInfo => {
  const record = readRecord(value);
  return {
    propertyId:
      typeof record.propertyId === 'number' ? readInteger(record, 'propertyId', 1) : readInteger(record, 'id', 1),
    name: readString(record, 'name'),
    depositAmount: readInteger(record, 'depositAmount'),
    monthlyRentAmount: readInteger(record, 'monthlyRentAmount'),
    discoverySource: parseDiscoverySource(record.discoverySource),
    location: parsePropertyLocation(record),
    updatedAt:
      typeof record.updatedAt === 'string'
        ? readUtcDateTime(record, 'updatedAt')
        : typeof record.createdAt === 'string'
          ? readUtcDateTime(record, 'createdAt')
          : null,
    lastActivityAt: typeof record.lastActivityAt === 'string' ? readUtcDateTime(record, 'lastActivityAt') : null,
  };
};

export const parsePropertyMemoDocument = (value: unknown): PropertyMemoDocument => {
  const record = readRecord(value);
  return {
    propertyId: readInteger(record, 'propertyId', 1),
    items: readArray(record, 'items')
      .map((item) => {
        const itemRecord = readRecord(item);
        return {
          ...(itemRecord.propertyMemoItemId === undefined
            ? {}
            : { propertyMemoItemId: readInteger(itemRecord, 'propertyMemoItemId', 1) }),
          systemMemoItemId: readInteger(itemRecord, 'systemMemoItemId', 1),
          label: readString(itemRecord, 'label'),
          displayOrder: readInteger(itemRecord, 'displayOrder', 1),
          content: readString(itemRecord, 'content', { allowEmpty: true, maximumCodePoints: 100 }),
        };
      })
      .sort((a, b) => a.displayOrder - b.displayOrder),
    freeMemo: readString(record, 'freeMemo', { allowEmpty: true, maximumCodePoints: 2_000 }),
  };
};

const parsePropertyChecklistProgress = (value: unknown): PropertyChecklistProgress => {
  const record = readRecord(value);
  return {
    totalCount: readInteger(record, 'totalCount'),
    completedCount: readInteger(record, 'completedCount'),
    goodCount: readInteger(record, 'goodCount'),
    cautionCount: readInteger(record, 'cautionCount'),
    unconfirmedCount: readInteger(record, 'unconfirmedCount'),
    progressRate: readInteger(record, 'progressRate'),
  };
};

const parsePropertyChecklistStageSummary = (value: unknown): PropertyChecklistStageSummary => {
  const stageRecord = readRecord(value);
  const stage = readString(stageRecord, 'stage');
  if (stage !== 'ONLINE_PHONE' && stage !== 'ON_SITE' && stage !== 'PRE_CONTRACT') {
    throw new Error('체크리스트 단계가 올바르지 않습니다.');
  }
  return {
    stage,
    applied: readBoolean(stageRecord, 'applied'),
    propertyChecklistId: readNullableInteger(stageRecord, 'propertyChecklistId', 1),
    checklistName: readNullableString(stageRecord, 'checklistName'),
    sourceChecklistId: readNullableInteger(stageRecord, 'sourceChecklistId', 1),
    progress: parsePropertyChecklistProgress(stageRecord.progress),
  };
};

export const parsePropertyChecklistOverview = (value: unknown): PropertyChecklistOverview => {
  const record = readRecord(value);
  return {
    propertyId: readInteger(record, 'propertyId', 1),
    overallProgress: parsePropertyChecklistProgress(record.overallProgress),
    stages: readArray(record, 'stages').map(parsePropertyChecklistStageSummary),
  };
};

const parsePropertyChecklistItemStatus = (value: unknown): PropertyChecklistItemStatus => {
  if (value !== 'UNCONFIRMED' && value !== 'GOOD' && value !== 'CAUTION') {
    throw new Error('체크리스트 항목 상태가 올바르지 않습니다.');
  }
  return value;
};

export const parsePropertyChecklistDetail = (value: unknown): PropertyChecklistDetail => {
  const record = readRecord(value);
  const stage = readString(record, 'stage');
  if (stage !== 'ONLINE_PHONE' && stage !== 'ON_SITE' && stage !== 'PRE_CONTRACT') {
    throw new Error('체크리스트 단계가 올바르지 않습니다.');
  }
  return {
    propertyChecklistId: readInteger(record, 'id', 1),
    propertyId: readInteger(record, 'propertyId', 1),
    sourceChecklistId: readNullableInteger(record, 'sourceChecklistId', 1),
    checklistName: readString(record, 'checklistName'),
    stage,
    items: readArray(record, 'items')
      .map((value) => {
        const item = readRecord(value);
        return {
          itemId: readInteger(item, 'id', 1),
          systemCheckItemId: readNullableInteger(item, 'systemCheckItemId', 1),
          question: readString(item, 'question', { maximumCodePoints: 200 }),
          displayOrder: readInteger(item, 'displayOrder', 1),
          status: parsePropertyChecklistItemStatus(item.status),
          memo: readString(item, 'memo', { allowEmpty: true, maximumCodePoints: 500 }),
        };
      })
      .sort((a, b) => a.displayOrder - b.displayOrder),
  };
};

const parsePropertyPhoto = (value: unknown): PropertyPhoto => {
  const record = readRecord(value);
  const contentType = readString(record, 'contentType');

  if (contentType !== 'image/jpeg' && contentType !== 'image/png' && contentType !== 'image/webp') {
    throw new Error('사진 형식 응답이 올바르지 않습니다.');
  }

  return {
    photoId: typeof record.photoId === 'number' ? readInteger(record, 'photoId', 1) : readInteger(record, 'id', 1),
    contentUrl: typeof record.contentUrl === 'string' ? readString(record, 'contentUrl') : readString(record, 'url'),
    contentType,
    sizeBytes: readInteger(record, 'sizeBytes', 1),
    createdAt: readUtcDateTime(record, 'createdAt'),
    representative: typeof record.representative === 'boolean' ? readBoolean(record, 'representative') : undefined,
  };
};

export const parsePropertyPhotoResponse = parsePropertyPhoto;

export const parsePropertyPhotoList = (value: unknown): PropertyPhotoList => {
  const record = readRecord(value);

  const photos = Array.isArray(record.photos) ? readArray(record, 'photos') : readArray(record, 'items');
  return { photos: photos.map(parsePropertyPhoto), totalCount: readInteger(record, 'totalCount') };
};

export const parseNoContent = (value: unknown): undefined => {
  if (value !== undefined && value !== null) {
    throw new Error('본문 없는 응답이 필요합니다.');
  }

  return undefined;
};
