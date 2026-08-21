import type { PublicConfig } from '../types/PublicConfig';
import type {
  PropertyBasicInfo,
  PropertyDetail,
  PropertyChecklistOverview,
  PropertyChecklistDetail,
  PropertyChecklistItemStatus,
  PropertyMemoDocument,
  PropertyPage,
} from '../types/Property';
import { ApiError, apiRequest } from './apiClient';
import type {
  PropertyInputDto,
  SavePropertyMemoDocumentRequestDto,
  UpdatePropertyRequestDto,
} from './dtos/PropertyDto';
import {
  parseNoContent,
  parsePropertyBasicInfo,
  parsePropertyDetail,
  parsePropertyChecklistOverview,
  parsePropertyChecklistDetail,
  parsePropertyMemoDocument,
  parsePropertyPage,
} from './propertyParsers';
import { readInteger, readRecord, readString } from './responseParsers';

export type PropertySearch = {
  query: string;
  page: number;
  size?: number;
};

export const fetchProperties = (
  config: PublicConfig,
  { query }: PropertySearch,
  signal?: AbortSignal,
): Promise<PropertyPage> => {
  const trimmedQuery = query.trim();
  return apiRequest({ config, path: '/api/properties', signal, parseData: parsePropertyPage }).then((result) => {
    if (trimmedQuery.length === 0) return result;
    const content = result.content.filter((property) => property.name.includes(trimmedQuery));
    return {
      ...result,
      content,
      size: Math.max(1, content.length),
      totalElements: content.length,
      totalPages: content.length === 0 ? 0 : 1,
    };
  });
};

const toPropertyRequest = ({ maintenanceFeeAmount: _maintenanceFeeAmount, ...request }: PropertyInputDto) => request;

export const createProperty = (config: PublicConfig, request: PropertyInputDto): Promise<PropertyBasicInfo> =>
  apiRequest({
    config,
    path: '/api/properties',
    method: 'POST',
    body: toPropertyRequest(request),
    parseData: parsePropertyBasicInfo,
  });

export const fetchPropertyDetail = (
  config: PublicConfig,
  propertyId: number,
  signal?: AbortSignal,
): Promise<PropertyDetail> =>
  apiRequest({ config, path: `/api/properties/${propertyId}`, signal, parseData: parsePropertyDetail });

export const updateProperty = (
  config: PublicConfig,
  propertyId: number,
  request: UpdatePropertyRequestDto,
): Promise<PropertyBasicInfo> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}`,
    method: 'PUT',
    body: toPropertyRequest(request),
    parseData: parsePropertyBasicInfo,
  });

export const fetchPropertyMemo = (
  config: PublicConfig,
  propertyId: number,
  signal?: AbortSignal,
): Promise<PropertyMemoDocument> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/memo`,
    signal,
    parseData: parsePropertyMemoDocument,
  });

export const initializePropertyMemo = (config: PublicConfig, propertyId: number): Promise<PropertyMemoDocument> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/memo`,
    method: 'POST',
    parseData: parsePropertyMemoDocument,
  });

export const fetchOrInitializePropertyMemo = async (
  config: PublicConfig,
  propertyId: number,
  signal?: AbortSignal,
): Promise<PropertyMemoDocument> => {
  try {
    const memo = await fetchPropertyMemo(config, propertyId, signal);
    if (memo.items.length > 0) return memo;
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 404) throw error;
  }

  return initializePropertyMemo(config, propertyId);
};

export const savePropertyMemoDocument = (
  config: PublicConfig,
  propertyId: number,
  request: SavePropertyMemoDocumentRequestDto,
): Promise<PropertyMemoDocument> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/memo`,
    method: 'PUT',
    body: request,
    parseData: parsePropertyMemoDocument,
  });

export const fetchPropertyChecklistOverview = (
  config: PublicConfig,
  propertyId: number,
  signal?: AbortSignal,
): Promise<PropertyChecklistOverview> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/checklists`,
    signal,
    parseData: parsePropertyChecklistOverview,
  });

export const fetchPropertyChecklistDetail = (
  config: PublicConfig,
  propertyId: number,
  propertyChecklistId: number,
  signal?: AbortSignal,
): Promise<PropertyChecklistDetail> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/checklists/${propertyChecklistId}`,
    signal,
    parseData: (value) => {
      const result = parsePropertyChecklistDetail(value);
      if (result.propertyId !== propertyId || result.propertyChecklistId !== propertyChecklistId) {
        throw new Error('매물 체크리스트 응답이 요청과 다릅니다.');
      }
      return result;
    },
  });

export const updatePropertyChecklistItemStatus = (
  config: PublicConfig,
  propertyId: number,
  propertyChecklistId: number,
  itemId: number,
  status: PropertyChecklistItemStatus,
): Promise<{ itemId: number; status: PropertyChecklistItemStatus }> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/checklists/${propertyChecklistId}/items/${itemId}/status`,
    method: 'PATCH',
    body: { status },
    parseData: (value) => {
      const record = readRecord(value);
      const item = readRecord(record.item);
      const resultStatus = readString(item, 'status');
      if (resultStatus !== 'UNCONFIRMED' && resultStatus !== 'GOOD' && resultStatus !== 'CAUTION') {
        throw new Error('체크리스트 항목 상태 응답이 올바르지 않습니다.');
      }
      return { itemId: readInteger(item, 'id', 1), status: resultStatus };
    },
  });

export const updatePropertyChecklistItemMemo = (
  config: PublicConfig,
  propertyId: number,
  propertyChecklistId: number,
  itemId: number,
  memo: string,
): Promise<{ itemId: number; memo: string }> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/checklists/${propertyChecklistId}/items/${itemId}/memo`,
    method: 'PATCH',
    body: { memo },
    parseData: (value) => {
      const record = readRecord(value);
      const item = readRecord(record.item);
      return {
        itemId: readInteger(item, 'id', 1),
        memo: readString(item, 'memo', { allowEmpty: true, maximumCodePoints: 500 }),
      };
    },
  });

export const removeProperty = (config: PublicConfig, propertyId: number): Promise<undefined> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}`,
    method: 'DELETE',
    parseData: parseNoContent,
  });
