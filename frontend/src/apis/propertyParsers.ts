import type {
  DiscoverySource,
  PropertyBasicInfo,
  PropertyChecklistOverview,
  PropertyChecklistProgress,
  PropertyChecklistDetail,
  PropertyChecklistItemStatus,
  PropertyDetail,
  PropertyMemoDocument,
  PropertyPage,
  PropertyPhoto,
  PropertyPhotoList,
  PropertyPhotoPreview,
  PropertySummary,
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
  const record = readRecord(value);
  const type = readString(record, 'type');

  if (type !== 'URL' && type !== 'TEXT') {
    throw new Error('발견 경로 유형이 올바르지 않습니다.');
  }

  return { type, value: readString(record, 'value') };
};

const parsePropertySummary = (value: unknown): PropertySummary => {
  const record = readRecord(value);
  const representativePhoto = record.representativePhoto;
  const parsedRepresentativePhoto =
    representativePhoto === null
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
    representativePhoto: parsedRepresentativePhoto,
    progress: parsePropertyChecklistProgress(record.overallProgress),
    photoCount: parsedRepresentativePhoto === null ? 0 : 1,
    lastActivityAt: '1970-01-01T00:00:00Z',
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
    ? record.photos.map((photo): PropertyPhotoPreview => {
        const photoRecord = readRecord(photo);
        return {
          photoId: readInteger(photoRecord, 'id', 1),
          contentUrl: readString(photoRecord, 'url'),
          createdAt:
            typeof photoRecord.created_at === 'string'
              ? readUtcDateTime(photoRecord, 'created_at')
              : typeof photoRecord.createdAt === 'string'
                ? readUtcDateTime(photoRecord, 'createdAt')
                : '1970-01-01T00:00:00Z',
        };
      })
    : null;

  return {
    propertyId:
      typeof record.propertyId === 'number' ? readInteger(record, 'propertyId', 1) : readInteger(record, 'id', 1),
    name: readString(record, 'name'),
    depositAmount: readInteger(record, 'depositAmount'),
    monthlyRentAmount: readInteger(record, 'monthlyRentAmount'),
    maintenanceFeeAmount: 'maintenanceFeeAmount' in record ? readNullableInteger(record, 'maintenanceFeeAmount') : null,
    discoverySource: parseDiscoverySource(record.discoverySource),
    photoPreview: photos === null ? parsePhotoPreview(record.photoPreview) : { totalCount: photos.length, photos },
    createdAt: typeof record.createdAt === 'string' ? readUtcDateTime(record, 'createdAt') : '1970-01-01T00:00:00Z',
    updatedAt: typeof record.updatedAt === 'string' ? readUtcDateTime(record, 'updatedAt') : '1970-01-01T00:00:00Z',
    lastActivityAt:
      typeof record.lastActivityAt === 'string' ? readUtcDateTime(record, 'lastActivityAt') : '1970-01-01T00:00:00Z',
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
    maintenanceFeeAmount: 'maintenanceFeeAmount' in record ? readNullableInteger(record, 'maintenanceFeeAmount') : null,
    discoverySource: parseDiscoverySource(record.discoverySource),
    updatedAt:
      typeof record.updatedAt === 'string'
        ? readUtcDateTime(record, 'updatedAt')
        : typeof record.createdAt === 'string'
          ? readUtcDateTime(record, 'createdAt')
          : null,
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
          propertyMemoItemId: readInteger(itemRecord, 'propertyMemoItemId', 1),
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

export const parsePropertyChecklistOverview = (value: unknown): PropertyChecklistOverview => {
  const record = readRecord(value);
  return {
    propertyId: readInteger(record, 'propertyId', 1),
    overallProgress: parsePropertyChecklistProgress(record.overallProgress),
    stages: readArray(record, 'stages').map((stageValue) => {
      const stageRecord = readRecord(stageValue);
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
    }),
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
          systemCheckItemId: readInteger(item, 'systemCheckItemId', 1),
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
