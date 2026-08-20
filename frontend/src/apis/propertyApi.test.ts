import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { setAuthentication } from '../app/authStore';
import { server } from '../test/server';
import { errorEnvelope, photoFixture, propertyDetailFixture, successEnvelope } from '../test/propertyFixtures';
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
  removeProperty,
  savePropertyMemo,
  savePropertyPreVisitMemo,
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
                progress: {
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
                progress: {
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

  it('API-102에 필수·선택 필드를 보내고 서버의 URL 분류를 사용한다', async () => {
    authenticate();
    let requestBody: unknown;
    server.use(
      http.post(`${config.apiBaseUrl}/api/properties`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            name: '신림역 원룸',
            depositAmount: 0,
            monthlyRentAmount: 550_000,
            maintenanceFeeAmount: null,
            discoverySource: { type: 'URL', value: 'https://example.com/home' },
            createdAt: '2026-08-10T07:30:00Z',
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
      maintenanceFeeAmount: null,
      discoverySource: 'https://example.com/home',
    });
    expect(result.discoverySource.type).toBe('URL');
  });

  it('API-103 상세 응답의 메모·요약·삭제 영향을 검증한다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailFixture)),
      ),
    );

    const detail = await fetchPropertyDetail(config, 10);
    expect(detail.memo.content).toBe('채광 다시 확인');
    expect(detail.memo.additionalMemo).toBe('채광 다시 확인');
    expect(detail.memo.viewingSchedule).toBe('8월 20일 오후 2시 방문');
    expect(detail.deletionImpact).toEqual({ visitCount: 2, photoCount: 2, activeChecklistCount: 1 });
  });

  it('API-103은 nullable savedAt을 보존하고 content와 additionalMemo 불일치·필드 누락을 거부한다', async () => {
    authenticate();
    let response: unknown = {
      ...propertyDetailFixture,
      memo: { ...propertyDetailFixture.memo, savedAt: null },
    };
    server.use(http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(response))));

    await expect(fetchPropertyDetail(config, 10)).resolves.toMatchObject({ memo: { savedAt: null } });

    response = {
      ...propertyDetailFixture,
      memo: { ...propertyDetailFixture.memo, content: '다른 값', savedAt: null },
    };
    await expect(fetchPropertyDetail(config, 10)).rejects.toMatchObject({ kind: 'invalid-response' });

    const memoWithoutRequiredField: Partial<typeof propertyDetailFixture.memo> = { ...propertyDetailFixture.memo };
    delete memoWithoutRequiredField.viewingSchedule;
    response = { ...propertyDetailFixture, memo: memoWithoutRequiredField };
    await expect(fetchPropertyDetail(config, 10)).rejects.toMatchObject({ kind: 'invalid-response' });
  });

  it('API-104에는 전체 필드와 선택 필드의 null을 보낸다', async () => {
    authenticate();
    let requestBody: unknown;
    server.use(
      http.put(`${config.apiBaseUrl}/api/properties/10`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            name: '신림역 원룸',
            depositAmount: 10_000_000,
            monthlyRentAmount: 530_000,
            maintenanceFeeAmount: null,
            discoverySource: { type: 'TEXT', value: '중개사 추천' },
            updatedAt: '2026-08-11T01:00:00Z',
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
      maintenanceFeeAmount: null,
      discoverySource: null,
    });
  });

  it('API-105와 API-204의 204 응답을 JSON으로 파싱하지 않는다', async () => {
    authenticate();
    server.use(
      http.delete(`${config.apiBaseUrl}/api/properties/10`, () => new HttpResponse(null, { status: 204 })),
      http.delete(`${config.apiBaseUrl}/api/properties/10/photos/81`, () => new HttpResponse(null, { status: 204 })),
    );

    await expect(removeProperty(config, 10)).resolves.toBeUndefined();
    await expect(removePropertyPhoto(config, 10, 81)).resolves.toBeUndefined();
  });

  it('API-106 메모 요청에 expectedVersion 없이 빈 문자열을 보낼 수 있다', async () => {
    authenticate();
    let requestBody: unknown;
    server.use(
      http.put(`${config.apiBaseUrl}/api/properties/10/memo`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            ...propertyDetailFixture.memo,
            additionalMemo: '',
            content: '',
            savedAt: '2026-08-11T01:00:00Z',
          }),
        );
      }),
    );

    const memo = await savePropertyMemo(config, 10, { content: '' });
    expect(requestBody).toEqual({ content: '' });
    expect(memo.content).toBe('');
  });

  it('API-106 v1.1은 여덟 구조화 필드만 전체 전송하고 Unicode 코드포인트 경계를 파싱한다', async () => {
    authenticate();
    let requestBody: unknown;
    const request = {
      viewingSchedule: '🏠'.repeat(200),
      moveInAvailability: '',
      provisionalDeposit: '',
      roomOptions: '',
      maintenanceAndUtilities: '',
      commuteTime: '',
      governmentSupport: '',
      additionalMemo: '채광 확인',
    };
    server.use(
      http.put(`${config.apiBaseUrl}/api/properties/10/memo`, async ({ request: incoming }) => {
        requestBody = await incoming.json();
        return HttpResponse.json(
          successEnvelope({
            ...request,
            content: request.additionalMemo,
            savedAt: '2026-08-11T01:00:00Z',
          }),
        );
      }),
    );

    await expect(savePropertyPreVisitMemo(config, 10, request)).resolves.toMatchObject(request);
    expect(requestBody).toEqual(request);
    expect(requestBody).not.toHaveProperty('content');
    expect(requestBody).not.toHaveProperty('expectedVersion');
  });

  it('API-106 응답의 구조화 필드가 코드포인트 제한을 넘거나 UTC가 아니면 거부한다', async () => {
    authenticate();
    let memo = {
      ...propertyDetailFixture.memo,
      viewingSchedule: '🏠'.repeat(201),
      savedAt: '2026-08-11T10:00:00+09:00',
    };
    server.use(http.put(`${config.apiBaseUrl}/api/properties/10/memo`, () => HttpResponse.json(successEnvelope(memo))));

    await expect(
      savePropertyPreVisitMemo(config, 10, {
        viewingSchedule: '',
        moveInAvailability: '',
        provisionalDeposit: '',
        roomOptions: '',
        maintenanceAndUtilities: '',
        commuteTime: '',
        governmentSupport: '',
        additionalMemo: '',
      }),
    ).rejects.toMatchObject({ kind: 'invalid-response' });

    memo = { ...propertyDetailFixture.memo, viewingSchedule: '', savedAt: '2026-08-11T10:00:00+09:00' };
    await expect(
      savePropertyPreVisitMemo(config, 10, {
        viewingSchedule: '',
        moveInAvailability: '',
        provisionalDeposit: '',
        roomOptions: '',
        maintenanceAndUtilities: '',
        commuteTime: '',
        governmentSupport: '',
        additionalMemo: '',
      }),
    ).rejects.toMatchObject({ kind: 'invalid-response' });
  });

  it('사진 조회는 사용하되 미구현 업로드는 실패를 그대로 드러낸다', async () => {
    authenticate();
    let formEntries: string[] = [];
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ photos: [photoFixture], totalCount: 1 })),
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
        return HttpResponse.json(successEnvelope({ ...photoFixture, representative: true }));
      }),
    );

    const photo = await setRepresentativePropertyPhoto(config, 10, 81);
    expect(photo.representative).toBe(true);
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
