import type { PublicConfig } from '../types/PublicConfig';
import type {
  PropertyBasicInfo,
  PropertyDetail,
  PropertyMemo,
  PropertyPage,
  PropertyPreVisitMemo,
} from '../types/Property';
import { apiRequest } from './apiClient';
import type {
  PropertyInputDto,
  SavePropertyMemoRequestDto,
  SavePropertyPreVisitMemoRequestDto,
  UpdatePropertyRequestDto,
} from './dtos/PropertyDto';
import {
  parseNoContent,
  parsePropertyBasicInfo,
  parsePropertyDetail,
  parsePropertyMemoResponse,
  parsePropertyPage,
} from './propertyParsers';

export type PropertySearch = {
  query: string;
  page: number;
  size?: number;
};

export const fetchProperties = (
  config: PublicConfig,
  { query, page, size = 20 }: PropertySearch,
  signal?: AbortSignal,
): Promise<PropertyPage> => {
  const search = new URLSearchParams({ page: String(page), size: String(size) });
  const trimmedQuery = query.trim();

  if (trimmedQuery.length > 0) {
    search.set('query', trimmedQuery);
  }

  return apiRequest({ config, path: `/api/properties?${search.toString()}`, signal, parseData: parsePropertyPage });
};

export const createProperty = (config: PublicConfig, request: PropertyInputDto): Promise<PropertyBasicInfo> =>
  apiRequest({ config, path: '/api/properties', method: 'POST', body: request, parseData: parsePropertyBasicInfo });

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
    method: 'PATCH',
    body: request,
    parseData: parsePropertyBasicInfo,
  });

export const removeProperty = (config: PublicConfig, propertyId: number): Promise<undefined> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}`,
    method: 'DELETE',
    parseData: parseNoContent,
  });

export const savePropertyPreVisitMemo = (
  config: PublicConfig,
  propertyId: number,
  request: SavePropertyPreVisitMemoRequestDto,
): Promise<PropertyPreVisitMemo> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/memo`,
    method: 'PUT',
    body: request,
    parseData: parsePropertyMemoResponse,
  });

/** @deprecated v1.0 화면 호환 전용이다. 신규 코드는 savePropertyPreVisitMemo를 사용한다. */
export const savePropertyMemo = (
  config: PublicConfig,
  propertyId: number,
  request: SavePropertyMemoRequestDto,
): Promise<PropertyMemo> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/memo`,
    method: 'PUT',
    body: request,
    parseData: parsePropertyMemoResponse,
  });
