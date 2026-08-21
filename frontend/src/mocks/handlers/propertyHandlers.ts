import { http, HttpResponse } from 'msw';
import {
  createMockPhotoBytes,
  emptyMemo,
  getMockMemosByProperty,
  getMockPhotosByProperty,
  getMockProperties,
  getProperty,
  notImplemented,
  propertyResponse,
  readPositiveInteger,
  setMockMemosByProperty,
  setMockPhotosByProperty,
  setMockProperties,
  success,
  failure,
  systemMemoItems,
} from '../mockStore';

export const propertyHandlers = [
  http.get('*/api/system-memo-items', () => success(systemMemoItems)),
  http.get('*/api/properties', () =>
    success({
      totalCount: getMockProperties().length,
      items: [...getMockProperties()].sort((a, b) => b.id - a.id).map(propertyResponse),
    }),
  ),
  http.post('*/api/properties', async ({ request }) => {
    const body = (await request.json()) as {
      name: string;
      depositAmount?: number;
      monthlyRentAmount?: number;
      discoverySource?: string;
    };
    const id = Math.max(0, ...getMockProperties().map((property) => property.id)) + 1;
    const property = {
      id,
      name: body.name,
      depositAmount: body.depositAmount ?? 0,
      monthlyRentAmount: body.monthlyRentAmount ?? 0,
      maintenanceFeeAmount: null,
      discoverySource: body.discoverySource ?? null,
    };
    setMockProperties([...getMockProperties(), property]);
    setMockPhotosByProperty(new Map(getMockPhotosByProperty()).set(id, []));
    return success(propertyResponse(property), 201);
  }),
  http.get('*/api/properties/:propertyId', ({ params }) => {
    const property = getProperty(params.propertyId);
    return property === undefined ? failure('PROPERTY_NOT_FOUND', 404) : success(propertyResponse(property));
  }),
  http.put('*/api/properties/:propertyId', async ({ params, request }) => {
    const property = getProperty(params.propertyId);
    if (property === undefined) return failure('PROPERTY_NOT_FOUND', 404);
    const body = (await request.json()) as {
      name: string;
      depositAmount?: number;
      monthlyRentAmount?: number;
      discoverySource?: string;
    };
    const updated = {
      ...property,
      name: body.name,
      depositAmount: body.depositAmount ?? 0,
      monthlyRentAmount: body.monthlyRentAmount ?? 0,
      discoverySource: body.discoverySource ?? null,
    };
    setMockProperties(getMockProperties().map((candidate) => (candidate.id === updated.id ? updated : candidate)));
    const response = { ...updated, maintenanceFeeAmount: undefined };
    return success(response);
  }),
  http.delete('*/api/properties/:propertyId', ({ params }) => {
    const property = getProperty(params.propertyId);
    if (property === undefined) return failure('PROPERTY_NOT_FOUND', 404);
    setMockProperties(getMockProperties().filter((candidate) => candidate.id !== property.id));
    return new HttpResponse(null, { status: 200 });
  }),
  http.get('*/api/properties/:propertyId/memo', ({ params }) => {
    const property = getProperty(params.propertyId);
    if (property === undefined) return failure('PROPERTY_NOT_FOUND', 404);
    const memo = getMockMemosByProperty().get(property.id);
    return memo === undefined ? failure('MEMO_NOT_FOUND', 404) : success(memo);
  }),
  http.post('*/api/properties/:propertyId/memo', ({ params }) => {
    const property = getProperty(params.propertyId);
    if (property === undefined) return failure('PROPERTY_NOT_FOUND', 404);
    const memo = getMockMemosByProperty().get(property.id) ?? emptyMemo(property.id);
    setMockMemosByProperty(new Map(getMockMemosByProperty()).set(property.id, memo));
    return success(memo);
  }),
  http.put('*/api/properties/:propertyId/memo', async ({ params, request }) => {
    const property = getProperty(params.propertyId);
    if (property === undefined) return failure('PROPERTY_NOT_FOUND', 404);
    const body = (await request.json()) as {
      items?: Array<{ systemMemoItemId: number; content: string }>;
      freeMemo?: string;
    };
    const current = getMockMemosByProperty().get(property.id) ?? emptyMemo(property.id);
    const requestedContent = new Map(body.items?.map((item) => [item.systemMemoItemId, item.content]) ?? []);
    const memo = {
      propertyId: property.id,
      items: current.items
        .filter((item) => requestedContent.has(item.systemMemoItemId))
        .map((item) => ({ ...item, content: requestedContent.get(item.systemMemoItemId) ?? '' })),
      freeMemo: body.freeMemo ?? '',
    };
    setMockMemosByProperty(new Map(getMockMemosByProperty()).set(property.id, memo));
    return success(memo);
  }),
  http.get('*/api/properties/:propertyId/photos', ({ params }) => {
    const property = getProperty(params.propertyId);
    if (property === undefined) return failure('PROPERTY_NOT_FOUND', 404);
    const items = getMockPhotosByProperty().get(property.id) ?? [];
    return success({ propertyId: property.id, totalCount: items.length, items });
  }),
  http.post('*/api/properties/:propertyId/photos', notImplemented),
  http.delete('*/api/properties/:propertyId/photos/:photoId', ({ params }) => {
    const property = getProperty(params.propertyId);
    const photoId = readPositiveInteger(params.photoId);
    if (property === undefined || photoId === null) return failure('PHOTO_NOT_FOUND', 404);
    const current = getMockPhotosByProperty().get(property.id) ?? [];
    if (!current.some((photo) => photo.id === photoId)) return failure('PHOTO_NOT_FOUND', 404);
    const remaining = current.filter((photo) => photo.id !== photoId);
    if (remaining.length > 0 && !remaining.some((photo) => photo.representative)) {
      remaining[0] = { ...remaining[0], representative: true };
    }
    setMockPhotosByProperty(new Map(getMockPhotosByProperty()).set(property.id, remaining));
    return new HttpResponse(null, { status: 200 });
  }),
  http.put('*/api/properties/:propertyId/photos/:photoId/representative', ({ params }) => {
    const property = getProperty(params.propertyId);
    const photoId = readPositiveInteger(params.photoId);
    if (property === undefined || photoId === null) return failure('PHOTO_NOT_FOUND', 404);
    const current = getMockPhotosByProperty().get(property.id) ?? [];
    if (!current.some((photo) => photo.id === photoId)) return failure('PHOTO_NOT_FOUND', 404);
    const updated = current.map((photo) => ({ ...photo, representative: photo.id === photoId }));
    setMockPhotosByProperty(new Map(getMockPhotosByProperty()).set(property.id, updated));
    return new HttpResponse(null, { status: 200 });
  }),
  http.get('*/api/properties/:propertyId/photos/:photoId/content', ({ params }) => {
    const photoId = readPositiveInteger(params.photoId) ?? 81;
    return new HttpResponse(createMockPhotoBytes(photoId), { headers: { 'Content-Type': 'image/png' } });
  }),
];
