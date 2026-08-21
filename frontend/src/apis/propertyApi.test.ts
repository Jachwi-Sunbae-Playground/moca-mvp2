import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { setAuthentication } from '../app/authStore';
import { server } from '../test/server';
import { errorEnvelope, photoFixture, successEnvelope } from '../test/propertyFixtures';
import type { PublicConfig } from '../types/PublicConfig';
import { getPropertyErrorMessage } from './propertyErrorMessages';
import {
  fetchPropertyPhotoContent,
  fetchPropertyPhotos,
  removePropertyPhoto,
  setRepresentativePropertyPhoto,
  uploadPropertyPhoto,
} from './photoApi';
import {
  createProperty,
  fetchProperties,
  fetchPropertyDetail,
  fetchPropertyMemo,
  removeProperty,
  savePropertyMemoDocument,
  updateProperty,
} from './propertyApi';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  googleClientId: 'test-client',
  googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
};

const authenticate = () => setAuthentication({ accessToken: 'memory-token', tokenType: 'Bearer', expiresIn: 60 });

describe('FE-2 API 경계', () => {
  it('매물 목록은 서버에 검색·페이지 쿼리를 보내지 않고 클라이언트에서 이름을 검색한다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, ({ request }) => {
        const url = new URL(request.url);
        expect(url.search).toBe('');
        expect(request.headers.get('Authorization')).toBe('Bearer memory-token');
        return HttpResponse.json(
          successEnvelope({
            totalCount: 2,
            items: [
              {
                id: 10,
                name: '신림역 원룸',
                depositAmount: 10_000_000,
                monthlyRentAmount: 550_000,
                discoverySource: 'https://example.com/listings/10',
                representativePhoto: null,
                overallProgress: {
                  totalCount: 0,
                  completedCount: 0,
                  goodCount: 0,
                  cautionCount: 0,
                  unconfirmedCount: 0,
                  progressRate: 0,
                },
              },
              {
                id: 11,
                name: '망원동 투룸',
                depositAmount: 20_000_000,
                monthlyRentAmount: 700_000,
                discoverySource: null,
                representativePhoto: null,
                overallProgress: {
                  totalCount: 0,
                  completedCount: 0,
                  goodCount: 0,
                  cautionCount: 0,
                  unconfirmedCount: 0,
                  progressRate: 0,
                },
              },
            ],
          }),
        );
      }),
    );

    const result = await fetchProperties(config, { query: '  신림  ', page: 2 });
    expect(result.content[0]?.name).toBe('신림역 원룸');
  });

  it('매물 생성은 Swagger에 정의된 필드만 보내고 서버의 URL 분류를 사용한다', async () => {
    authenticate();
    let requestBody: unknown;
    server.use(
      http.post(`${config.apiBaseUrl}/api/properties`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            id: 10,
            name: '신림역 원룸',
            depositAmount: 0,
            monthlyRentAmount: 550_000,
            discoverySource: 'https://example.com/home',
            photos: [],
            overallProgress: {
              totalCount: 0,
              completedCount: 0,
              goodCount: 0,
              cautionCount: 0,
              unconfirmedCount: 0,
              progressRate: 0,
            },
          }),
          { status: 201 },
        );
      }),
    );

    const result = await createProperty(config, {
      name: '신림역 원룸',
      depositAmount: 0,
      monthlyRentAmount: 550_000,
      maintenanceFeeAmount: null,
      discoverySource: 'https://example.com/home',
    });

    expect(requestBody).toEqual({
      name: '신림역 원룸',
      depositAmount: 0,
      monthlyRentAmount: 550_000,
      discoverySource: 'https://example.com/home',
    });
    expect(result.discoverySource.type).toBe('URL');
  });

  it('매물 상세 응답의 기본 정보·사진·전체 진행률을 읽는다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(
          successEnvelope({
            id: 10,
            name: '신림역 원룸',
            depositAmount: 10_000_000,
            monthlyRentAmount: 550_000,
            discoverySource: 'https://example.com/listings/10',
            photos: [{ id: 81, url: '/api/properties/10/photos/81/content', createdAt: '2026-08-10T07:35:00Z' }],
            overallProgress: {
              totalCount: 3,
              completedCount: 2,
              goodCount: 1,
              cautionCount: 1,
              unconfirmedCount: 1,
              progressRate: 67,
            },
          }),
        ),
      ),
    );

    const detail = await fetchPropertyDetail(config, 10);
    expect(detail).toMatchObject({
      propertyId: 10,
      photoPreview: { totalCount: 1, photos: [{ photoId: 81 }] },
    });
  });

  it('매물 수정은 Swagger에 정의된 전체 필드와 선택 필드의 null을 보낸다', async () => {
    authenticate();
    let requestBody: unknown;
    server.use(
      http.put(`${config.apiBaseUrl}/api/properties/10`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            id: 10,
            name: '신림역 원룸',
            depositAmount: 10_000_000,
            monthlyRentAmount: 530_000,
            discoverySource: null,
          }),
        );
      }),
    );

    await updateProperty(config, 10, {
      name: '신림역 원룸',
      depositAmount: 10_000_000,
      monthlyRentAmount: 530_000,
      maintenanceFeeAmount: null,
      discoverySource: null,
    });
    expect(requestBody).toEqual({
      name: '신림역 원룸',
      depositAmount: 10_000_000,
      monthlyRentAmount: 530_000,
      discoverySource: null,
    });
  });

  it('매물과 사진 삭제의 200 빈 응답을 JSON으로 파싱하지 않는다', async () => {
    authenticate();
    server.use(
      http.delete(`${config.apiBaseUrl}/api/properties/10`, () => new HttpResponse(null, { status: 200 })),
      http.delete(`${config.apiBaseUrl}/api/properties/10/photos/81`, () => new HttpResponse(null, { status: 200 })),
    );

    await expect(removeProperty(config, 10)).resolves.toBeUndefined();
    await expect(removePropertyPhoto(config, 10, 81)).resolves.toBeUndefined();
  });

  it('매물 메모는 매물 메모 항목 ID와 자유 메모를 조회하고 저장한다', async () => {
    authenticate();
    let requestBody: unknown;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10/memo`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            items: [{ propertyMemoItemId: 1001, systemMemoItemId: 1, label: '집 주소', displayOrder: 1, content: '' }],
            freeMemo: '',
          }),
        ),
      ),
      http.put(`${config.apiBaseUrl}/api/properties/10/memo`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            items: [
              {
                propertyMemoItemId: 1001,
                systemMemoItemId: 1,
                label: '집 주소',
                displayOrder: 1,
                content: '관악구 신림로',
              },
            ],
            freeMemo: '채광 확인',
          }),
        );
      }),
    );

    await expect(fetchPropertyMemo(config, 10)).resolves.toMatchObject({ items: [{ propertyMemoItemId: 1001 }] });
    const memo = await savePropertyMemoDocument(config, 10, {
      items: [{ propertyMemoItemId: 1001, content: '관악구 신림로' }],
      freeMemo: '채광 확인',
    });
    expect(requestBody).toEqual({
      items: [{ propertyMemoItemId: 1001, content: '관악구 신림로' }],
      freeMemo: '채광 확인',
    });
    expect(memo.freeMemo).toBe('채광 확인');
  });

  it('사진 조회는 사용하되 미구현 업로드는 실패를 그대로 드러낸다', async () => {
    authenticate();
    let formEntries: string[] = [];
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ propertyId: 10, items: [photoFixture], totalCount: 1 })),
      ),
      http.post(`${config.apiBaseUrl}/api/properties/10/photos`, async ({ request }) => {
        expect(request.headers.get('Content-Type')).toContain('multipart/form-data; boundary=');
        const formData = await request.formData();
        formEntries = [...formData.keys()];
        expect(formData.get('file')).toMatchObject({ type: 'image/jpeg', size: expect.any(Number) });
        return HttpResponse.json(errorEnvelope('NOT_IMPLEMENTED'), { status: 501 });
      }),
    );

    await expect(fetchPropertyPhotos(config, 10)).resolves.toMatchObject({ totalCount: 1 });
    const file = new File([new Uint8Array([1, 2, 3])], 'local-only.jpg', { type: 'image/jpeg' });
    await expect(uploadPropertyPhoto(config, 10, file)).rejects.toMatchObject({ status: 501, code: 'NOT_IMPLEMENTED' });
    expect(formEntries).toEqual(['file']);
  });

  it('API-203은 Bearer 헤더로 Blob을 읽고 contentUrl이나 query에 토큰을 넣지 않는다', async () => {
    authenticate();
    let requestedUrl = '';
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10/photos/81/content`, ({ request }) => {
        requestedUrl = request.url;
        expect(request.headers.get('Authorization')).toBe('Bearer memory-token');
        return new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } });
      }),
    );

    const blob = await fetchPropertyPhotoContent(config, '/api/properties/10/photos/81/content');
    expect(blob.type).toBe('image/jpeg');
    expect(requestedUrl).toBe('http://localhost:8080/api/properties/10/photos/81/content');
    expect(requestedUrl).not.toContain('memory-token');
  });

  it('대표 사진 지정은 사진 식별자와 Bearer 헤더를 사용한다', async () => {
    authenticate();
    let requestedUrl = '';
    server.use(
      http.put(`${config.apiBaseUrl}/api/properties/10/photos/81/representative`, ({ request }) => {
        requestedUrl = request.url;
        expect(request.headers.get('Authorization')).toBe('Bearer memory-token');
        return new HttpResponse(null, { status: 200 });
      }),
    );

    await expect(setRepresentativePropertyPhoto(config, 10, 81)).resolves.toBeUndefined();
    expect(requestedUrl).toBe('http://localhost:8080/api/properties/10/photos/81/representative');
  });

  it('공통 오류 code를 서버 message 대신 안전한 사용자 문구로 매핑한다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(errorEnvelope('PHOTO_READ_FAILED'), { status: 500 }),
      ),
    );

    const error = await fetchPropertyPhotos(config, 10).catch((caught: unknown) => caught);
    expect(getPropertyErrorMessage(error)).toBe('사진을 불러오지 못했습니다. 다시 시도해 주세요.');
    expect(getPropertyErrorMessage(error)).not.toContain('서버 내부 상세 메시지');
  });
});
