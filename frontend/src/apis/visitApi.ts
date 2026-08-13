import type { PublicConfig } from '../types/PublicConfig';
import type {
  VisitCompletionResult,
  VisitDetail,
  VisitItemMemoUpdateResult,
  VisitItemStatusUpdateResult,
  VisitItemUpdateResult,
  VisitPage,
} from '../types/Visit';
import { ApiError, apiRequest } from './apiClient';
import type {
  CompleteVisitRequestDto,
  UpdateVisitItemMemoRequestDto,
  UpdateVisitItemRequestDto,
  UpdateVisitItemStatusRequestDto,
} from './dtos/VisitDto';
import {
  parseVisitCompletion,
  parseVisitDetail,
  parseVisitItemMemoUpdate,
  parseVisitItemStatusUpdate,
  parseVisitPage,
} from './visitParsers';

export const fetchPropertyVisits = (
  config: PublicConfig,
  propertyId: number,
  page: number,
  size = 20,
  signal?: AbortSignal,
): Promise<VisitPage> => {
  const search = new URLSearchParams({ page: String(page), size: String(size) });
  return apiRequest({
    config,
    path: `/api/properties/${propertyId}/visits?${search.toString()}`,
    signal,
    parseData: parseVisitPage,
  });
};

export const startPropertyVisit = (config: PublicConfig, propertyId: number): Promise<VisitDetail> =>
  apiRequest({
    config,
    path: `/api/properties/${propertyId}/visits`,
    method: 'POST',
    parseData: parseVisitDetail,
  });

export const fetchVisitDetail = async (
  config: PublicConfig,
  visitId: number,
  signal?: AbortSignal,
): Promise<VisitDetail> => {
  const detail = await apiRequest({ config, path: `/api/visits/${visitId}`, signal, parseData: parseVisitDetail });
  if (detail.visitId !== visitId) throw new ApiError({ kind: 'invalid-response' });
  return detail;
};

export const updateVisitItemStatus = async (
  config: PublicConfig,
  visitId: number,
  visitItemId: number,
  request: UpdateVisitItemStatusRequestDto,
): Promise<VisitItemStatusUpdateResult> => {
  const result = await apiRequest({
    config,
    path: `/api/visits/${visitId}/items/${visitItemId}`,
    method: 'PATCH',
    body: request,
    parseData: parseVisitItemStatusUpdate,
  });
  if (result.item.visitItemId !== visitItemId) throw new ApiError({ kind: 'invalid-response' });
  return result;
};

export const updateVisitItemMemo = async (
  config: PublicConfig,
  visitId: number,
  visitItemId: number,
  request: UpdateVisitItemMemoRequestDto,
): Promise<VisitItemMemoUpdateResult> => {
  const result = await apiRequest({
    config,
    path: `/api/visits/${visitId}/items/${visitItemId}/memo`,
    method: 'PATCH',
    body: request,
    parseData: parseVisitItemMemoUpdate,
  });
  if (result.visitItemId !== visitItemId) throw new ApiError({ kind: 'invalid-response' });
  return result;
};

/** @deprecated v1.0 상태 CAS 호환 전용이다. 신규 코드는 updateVisitItemStatus를 사용한다. */
export const updateVisitItem = async (
  config: PublicConfig,
  visitId: number,
  visitItemId: number,
  request: UpdateVisitItemRequestDto,
): Promise<VisitItemUpdateResult> => {
  const result = await updateVisitItemStatus(config, visitId, visitItemId, {
    status: request.status,
    expectedStatusVersion: request.expectedVersion,
  });
  return {
    ...result,
    item: {
      visitItemId: result.item.visitItemId,
      status: result.item.status,
      version: result.item.statusVersion,
      savedAt: result.item.statusSavedAt,
    },
  };
};

export const completeVisit = (config: PublicConfig, visitId: number): Promise<VisitCompletionResult> => {
  const request: CompleteVisitRequestDto = { status: 'COMPLETED' };
  return apiRequest({
    config,
    path: `/api/visits/${visitId}`,
    method: 'PATCH',
    body: request,
    parseData: (value) => {
      const result = parseVisitCompletion(value);
      if (result.visitId !== visitId) throw new ApiError({ kind: 'invalid-response' });
      return result;
    },
  });
};
