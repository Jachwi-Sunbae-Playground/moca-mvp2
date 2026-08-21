import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, delay, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { setAuthentication } from './authStore';
import { queryClient } from './queryClient';
import AppRoutes from './AppRoutes';
import { server } from '../test/server';
import {
  errorEnvelope,
  memberFixture,
  photoFixture,
  propertyDetailFixture,
  propertyDetailResponseFixture,
  propertyPageFixture,
  propertyPhotoResponseFixture,
  propertySummaryFixture,
  secondPropertySummaryFixture,
  successEnvelope,
} from '../test/propertyFixtures';
import type { PublicConfig } from '../types/PublicConfig';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  googleClientId: 'test-client',
  googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
};

const detailWithoutPhotosFixture = {
  ...propertyDetailFixture,
  photoCount: 0,
  photoPreview: { totalCount: 0, photos: [] },
};
const detailWithoutPhotos = propertyDetailResponseFixture(detailWithoutPhotosFixture);

const renderAuthenticated = (path: string) => {
  setAuthentication({ accessToken: 'memory-token', tokenType: 'Bearer', expiresIn: 60 });
  server.use(http.get(`${config.apiBaseUrl}/api/members/me`, () => HttpResponse.json(successEnvelope(memberFixture))));

  return render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[path]}>
          <AppRoutes config={config} storage={window.sessionStorage} />
        </MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  );
};

describe('FE-2 매물 목록', () => {
  it('매물 0개와 검색 결과 없음을 구분한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(successEnvelope(propertyPageFixture([]))),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties');

    expect(await screen.findByText('아직 등록한 매물이 없어요.')).toBeInTheDocument();
    await user.type(screen.getByRole('textbox', { name: '매물 이름 검색' }), '없는 매물{Enter}');
    expect(await screen.findByText('검색 결과가 없어요.')).toBeInTheDocument();
  });

  it('매물 진행 현황을 표시하고 상태별로 필터링한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(successEnvelope(propertyPageFixture([propertySummaryFixture, secondPropertySummaryFixture]))),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties');

    expect(await screen.findByRole('link', { name: '신림역 원룸' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '망원동 투룸' })).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: '매물 목록' })).getAllByText('미완료')).toHaveLength(1);
    const firstPropertySummary = within(screen.getByRole('link', { name: '신림역 원룸' })).getByRole('list', {
      name: '체크리스트 진행 결과 집계',
    });
    expect(firstPropertySummary).toHaveTextContent('괜찮음 10');
    expect(firstPropertySummary).toHaveTextContent('주의 5');
    expect(firstPropertySummary).toHaveTextContent('미확인 7');

    await user.click(screen.getByRole('button', { name: '완료' }));
    expect(screen.queryByRole('link', { name: '신림역 원룸' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: '망원동 투룸' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '미완료' }));
    expect(screen.getByRole('link', { name: '신림역 원룸' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '망원동 투룸' })).not.toBeInTheDocument();
  });

  it('전체 개수와 서버가 반환한 순서를 목록에 반영한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(successEnvelope(propertyPageFixture([propertySummaryFixture, secondPropertySummaryFixture]))),
      ),
    );
    renderAuthenticated('/properties');

    expect(
      await screen.findByText((_, element) => element?.tagName === 'SPAN' && element.textContent === '전체 2'),
    ).toBeInTheDocument();
    const list = screen.getByRole('region', { name: '매물 목록' });
    expect(
      within(list)
        .getAllByRole('link')
        .map((link) => link.getAttribute('aria-label')),
    ).toEqual(['신림역 원룸', '망원동 투룸']);
  });

  it('검색은 목록을 다시 요청하지 않고 받아온 매물 이름을 기준으로 적용한다', async () => {
    let listCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () => {
        listCalls += 1;
        return HttpResponse.json(
          successEnvelope(propertyPageFixture([propertySummaryFixture, secondPropertySummaryFixture])),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties');
    await screen.findByRole('link', { name: '신림역 원룸' });
    const callsAfterInitialLoad = listCalls;
    const searchbox = await screen.findByRole('textbox', { name: '매물 이름 검색' });

    await user.type(searchbox, '  망원  ');
    await user.keyboard('{Enter}');

    expect(await screen.findByRole('link', { name: '망원동 투룸' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '신림역 원룸' })).not.toBeInTheDocument();
    expect(listCalls).toBe(callsAfterInitialLoad);
  });

  it('최초 목록 실패 후 재시도할 수 있다', async () => {
    let shouldFail = true;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () => {
        return shouldFail
          ? HttpResponse.error()
          : HttpResponse.json(successEnvelope(propertyPageFixture([propertySummaryFixture])));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties');

    expect(await screen.findByText('매물 목록을 불러오지 못했어요.')).toBeInTheDocument();
    shouldFail = false;
    await user.click(screen.getByRole('button', { name: '다시 시도' }));
    expect(await screen.findByRole('link', { name: '신림역 원룸' })).toBeInTheDocument();
  });
});

describe('FE-2 등록·수정·메모', () => {
  it('등록 필수값을 검증하고 성공 후 생성된 상세로 이동한다', async () => {
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
            discoverySource: '중개사 추천',
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
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(
          successEnvelope({
            ...detailWithoutPhotos,
            depositAmount: 0,
            discoverySource: '중개사 추천',
          }),
        ),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/new');

    await user.click(await screen.findByRole('button', { name: '매물 등록' }));
    expect(screen.getByText('매물을 구분할 이름을 입력해 주세요.')).toBeInTheDocument();

    await user.type(screen.getByLabelText('이름'), ' 신림역 원룸 ');
    await user.type(screen.getByLabelText('보증금'), '0');
    await user.type(screen.getByLabelText('월세'), '550000');
    await user.type(screen.getByLabelText('확인한 곳'), ' 중개사 추천 ');
    await user.click(screen.getByRole('button', { name: '매물 등록' }));

    expect(await screen.findByRole('heading', { name: '신림역 원룸', level: 1 })).toBeInTheDocument();
    expect(
      screen.getByText(
        (_, element) => element?.tagName === 'P' && element.textContent === '보증금 0원 / 월세 550,000원',
      ),
    ).toBeInTheDocument();
    expect(requestBody).toEqual({
      name: '신림역 원룸',
      depositAmount: 0,
      monthlyRentAmount: 550_000,
      discoverySource: '중개사 추천',
    });
  });

  it('등록 실패 시 입력을 유지하고 서버 validation 필드를 가까이 표시한다', async () => {
    server.use(
      http.post(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(errorEnvelope('INVALID_REQUEST', [{ field: 'name', reason: 'internal validation' }]), {
          status: 400,
        }),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/new');

    await user.type(await screen.findByLabelText('이름'), '유지할 이름');
    await user.type(screen.getByLabelText('보증금'), '1000');
    await user.type(screen.getByLabelText('월세'), '55');
    await user.type(screen.getByLabelText('확인한 곳'), 'https://example.com');
    await user.click(screen.getByRole('button', { name: '매물 등록' }));

    expect(await screen.findByText('서버에서 매물 이름을 확인하지 못했습니다.')).toBeInTheDocument();
    expect(screen.getByLabelText('이름')).toHaveValue('유지할 이름');
    expect(document.body.textContent).not.toContain('internal validation');
  });

  it('수정은 전체 필드를 보내고 변경이 없으면 API를 호출하지 않는다', async () => {
    let updateCalls = 0;
    let updateBody: unknown;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.put(`${config.apiBaseUrl}/api/properties/10`, async ({ request }) => {
        updateCalls += 1;
        updateBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            id: 10,
            name: propertyDetailFixture.name,
            depositAmount: propertyDetailFixture.depositAmount,
            monthlyRentAmount: 530_000,
            discoverySource: propertyDetailFixture.discoverySource.value,
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/edit');

    await user.click(await screen.findByRole('button', { name: '변경사항 저장' }));
    expect(screen.getByText(/변경된 내용이 없어/)).toBeInTheDocument();
    expect(updateCalls).toBe(0);

    await user.clear(screen.getByLabelText('월세'));
    await user.type(screen.getByLabelText('월세'), '530000');
    await user.click(screen.getByRole('button', { name: '변경사항 저장' }));
    expect(await screen.findByRole('heading', { name: '신림역 원룸', level: 1 })).toBeInTheDocument();
    expect(updateBody).toEqual({
      name: propertyDetailFixture.name,
      depositAmount: propertyDetailFixture.depositAmount,
      monthlyRentAmount: 530_000,
      discoverySource: propertyDetailFixture.discoverySource.value,
    });
  });

  it('상세에서는 메모 입력 없이 작성된 항목만 강조해 링크한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.get(`${config.apiBaseUrl}/api/properties/10/memo`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            items: [
              {
                propertyMemoItemId: 101,
                systemMemoItemId: 1,
                label: '집 주소',
                displayOrder: 1,
                content: '관악구 신림로 12길',
              },
              { propertyMemoItemId: 102, systemMemoItemId: 2, label: '입주 가능일', displayOrder: 2, content: '' },
            ],
            freeMemo: '',
          }),
        ),
      ),
    );
    renderAuthenticated('/properties/10');

    expect(await screen.findByText('집 주소')).toBeInTheDocument();
    expect(screen.getByText('입주 가능일')).toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /메모 작성/ })).toHaveAttribute('href', '/properties/10/memo');
  });

  it('별도 메모 화면에서 기본 양식과 자유 메모를 저장한 뒤 상세로 이동한다', async () => {
    let requestBody: unknown;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.get(`${config.apiBaseUrl}/api/properties/10/memo`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            items: [
              { propertyMemoItemId: 101, systemMemoItemId: 1, label: '집 주소', displayOrder: 1, content: '' },
              { propertyMemoItemId: 102, systemMemoItemId: 2, label: '입주 가능일', displayOrder: 2, content: '' },
            ],
            freeMemo: '',
          }),
        ),
      ),
      http.put(`${config.apiBaseUrl}/api/properties/10/memo`, async ({ request }) => {
        requestBody = await request.json();
        const body = requestBody as {
          items: Array<{ propertyMemoItemId: number; content: string }>;
          freeMemo: string;
        };
        return HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            items: body.items.map((item, index) => ({
              ...item,
              systemMemoItemId: index + 1,
              label: index === 0 ? '집 주소' : '입주 가능일',
              displayOrder: index + 1,
            })),
            freeMemo: body.freeMemo,
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/memo');

    await user.type(await screen.findByRole('textbox', { name: '집 주소' }), '관악구 신림로 12길');
    await user.type(screen.getByRole('textbox', { name: '추가 메모' }), '채광을 다시 확인하기');
    await user.click(screen.getByRole('button', { name: '메모 저장' }));

    expect(await screen.findByRole('heading', { name: '신림역 원룸', level: 1 })).toBeInTheDocument();
    expect(requestBody).toEqual({
      items: [
        { propertyMemoItemId: 101, content: '관악구 신림로 12길' },
        { propertyMemoItemId: 102, content: '' },
      ],
      freeMemo: '채광을 다시 확인하기',
    });
  });

  it('메모 저장 실패 시 작성 내용을 유지해 다시 저장할 수 있다', async () => {
    let saveAttempts = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.get(`${config.apiBaseUrl}/api/properties/10/memo`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            items: [{ propertyMemoItemId: 101, systemMemoItemId: 1, label: '집 주소', displayOrder: 1, content: '' }],
            freeMemo: '',
          }),
        ),
      ),
      http.put(`${config.apiBaseUrl}/api/properties/10/memo`, async ({ request }) => {
        saveAttempts += 1;
        const body = await request.json();
        if (saveAttempts === 1) return HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 });
        const memoBody = body as {
          items: Array<{ propertyMemoItemId: number; content: string }>;
          freeMemo: string;
        };
        return HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            items: memoBody.items.map((item) => ({
              ...item,
              systemMemoItemId: 1,
              label: '집 주소',
              displayOrder: 1,
            })),
            freeMemo: memoBody.freeMemo,
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/memo');
    const memo = await screen.findByRole('textbox', { name: '추가 메모' });

    await user.type(memo, '작성 중인 내용');
    await user.click(screen.getByRole('button', { name: '메모 저장' }));
    expect(await screen.findByText(/메모를 저장하지 못했어요/)).toBeInTheDocument();
    expect(memo).toHaveValue('작성 중인 내용');
    await user.click(screen.getByRole('button', { name: '메모 저장' }));
    expect(await screen.findByRole('heading', { name: '신림역 원룸', level: 1 })).toBeInTheDocument();
    expect(saveAttempts).toBe(2);
  });
});

describe('FE-2 사진과 삭제 확인', () => {
  it('상세 사진은 큰 화면에서 넘겨 보고 별도 사진 화면에서 관리한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailResponseFixture())),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            items: [
              propertyPhotoResponseFixture(),
              propertyPhotoResponseFixture({
                ...photoFixture,
                photoId: 82,
                contentUrl: '/api/properties/10/photos/82/content',
              }),
            ],
            totalCount: 2,
          }),
        ),
      ),
      http.get(
        `${config.apiBaseUrl}/api/properties/10/photos/81/content`,
        () => new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
      http.get(
        `${config.apiBaseUrl}/api/properties/10/photos/82/content`,
        () => new HttpResponse(new Uint8Array([255, 216, 254]), { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
    );

    const user = userEvent.setup();
    const detailView = renderAuthenticated('/properties/10');
    const photoButton = await screen.findByRole('button', { name: '신림역 원룸 사진 1 크게 보기' });
    await user.click(photoButton);

    expect(await screen.findByRole('dialog', { name: '신림역 원룸 사진 크게 보기' })).toBeInTheDocument();
    expect(await screen.findByRole('img', { name: '신림역 원룸 사진 1' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '다음 사진' }));
    expect(await screen.findByRole('img', { name: '신림역 원룸 사진 2' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '사진 크게 보기 닫기' }));
    expect(screen.getByRole('link', { name: /사진 관리/ })).toHaveAttribute('href', '/properties/10/photos');
    detailView.unmount();

    renderAuthenticated('/properties/10/photos');
    expect(await screen.findByRole('heading', { name: '사진 관리' })).toBeInTheDocument();
    expect(screen.getByLabelText('사진 파일 선택')).toBeEnabled();
    expect(await screen.findByRole('img', { name: '업로드 순 1번째 사진' })).toBeInTheDocument();
  });

  it('빈 사진 목록과 잘못된 형식·10MiB 초과를 업로드 전에 안내한다', async () => {
    let uploadCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ propertyId: 10, items: [], totalCount: 0 })),
      ),
      http.post(`${config.apiBaseUrl}/api/properties/10/photos`, () => {
        uploadCalls += 1;
        return HttpResponse.json(successEnvelope(photoFixture), { status: 201 });
      }),
    );
    const user = userEvent.setup({ applyAccept: false });
    renderAuthenticated('/properties/10/photos');

    expect(await screen.findByText('등록한 사진이 없어요.')).toBeInTheDocument();
    const input = screen.getByLabelText(/사진 추가|파일 선택/, { selector: 'input[type="file"]' });
    await user.upload(input, [
      new File(['text'], 'wrong.txt', { type: 'text/plain' }),
      new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'large.jpg', { type: 'image/jpeg' }),
    ]);

    expect(await screen.findByText('JPEG, PNG 또는 WebP 사진을 선택해 주세요.')).toBeInTheDocument();
    expect(screen.getByText('사진 한 장은 10MiB 이하만 등록할 수 있습니다.')).toBeInTheDocument();
    expect(uploadCalls).toBe(0);
  });

  it('사진 30장 제한에 도달하면 파일 선택을 비활성화한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ propertyId: 10, items: [], totalCount: 30 })),
      ),
    );
    renderAuthenticated('/properties/10/photos');

    expect(await screen.findByText('사진 30장이 모두 등록되어 추가할 수 없어요.')).toBeInTheDocument();
    expect(screen.getByLabelText('사진 파일 선택')).toBeDisabled();
  });

  it('다중 파일을 순차 처리하고 일부 실패 후에도 나머지를 계속한다', async () => {
    let activeUploads = 0;
    let maxActiveUploads = 0;
    let uploadCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ propertyId: 10, items: [], totalCount: 0 })),
      ),
      http.post(`${config.apiBaseUrl}/api/properties/10/photos`, async () => {
        uploadCalls += 1;
        activeUploads += 1;
        maxActiveUploads = Math.max(maxActiveUploads, activeUploads);
        await delay(10);
        activeUploads -= 1;
        if (uploadCalls === 2) return HttpResponse.json(errorEnvelope('PHOTO_UPLOAD_FAILED'), { status: 500 });
        return HttpResponse.json(successEnvelope({ ...photoFixture, photoId: 80 + uploadCalls }), { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/photos');
    const input = await screen.findByLabelText(/사진 추가|파일 선택/, { selector: 'input[type="file"]' });

    await user.upload(
      input,
      [1, 2, 3].map((number) => new File([new Uint8Array([number])], `photo-${number}.jpg`, { type: 'image/jpeg' })),
    );
    await waitFor(() => expect(uploadCalls).toBe(3));
    expect(screen.queryByText('업로드 결과')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '결과 지우기' })).not.toBeInTheDocument();
    expect(screen.getByText('사진을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.')).toBeInTheDocument();
    expect(uploadCalls).toBe(3);
    expect(maxActiveUploads).toBe(1);
  });

  it('사진 삭제는 취소·성공을 구분하고 성공 전에는 화면에서 제거하지 않는다', async () => {
    let deleted = false;
    let deleteCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailResponseFixture())),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            items: deleted ? [] : [propertyPhotoResponseFixture()],
            totalCount: deleted ? 0 : 1,
          }),
        ),
      ),
      http.get(
        `${config.apiBaseUrl}/api/properties/10/photos/81/content`,
        () => new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
      http.delete(`${config.apiBaseUrl}/api/properties/10/photos/81`, () => {
        deleteCalls += 1;
        deleted = true;
        return new HttpResponse(null, { status: 200 });
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/photos');

    const deleteButton = await screen.findByRole('button', { name: '업로드 순 1번째 사진 삭제' });
    expect(screen.queryByRole('img', { name: '삭제할 업로드 순 1번째 사진' })).not.toBeInTheDocument();
    await user.click(deleteButton);
    const dialog = screen.getByRole('dialog', { name: '이 사진을 삭제할까요?' });
    expect(within(dialog).getByRole('img', { name: '삭제할 업로드 순 1번째 사진' })).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: '취소' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(deleteButton).toHaveFocus();
    expect(screen.queryByRole('img', { name: '삭제할 업로드 순 1번째 사진' })).not.toBeInTheDocument();

    await user.click(deleteButton);
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '취소' }));
    expect(deleteCalls).toBe(0);
    expect(screen.getByRole('img', { name: '업로드 순 1번째 사진' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '업로드 순 1번째 사진 삭제' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '사진 삭제' }));
    expect(await screen.findByText('등록한 사진이 없어요.')).toBeInTheDocument();
    expect(deleteCalls).toBe(1);
  });

  it('사진 관리에서 대표 사진을 지정하고 갱신된 상태를 표시한다', async () => {
    let representativePhotoId = 81;
    let representativeCalls = 0;
    const photos = [
      { ...photoFixture, representative: true },
      {
        ...photoFixture,
        photoId: 82,
        contentUrl: '/api/properties/10/photos/82/content',
        representative: false,
      },
    ];
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailResponseFixture())),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            items: photos.map((photo) =>
              propertyPhotoResponseFixture({ ...photo, representative: photo.photoId === representativePhotoId }),
            ),
            totalCount: photos.length,
          }),
        ),
      ),
      http.get(
        `${config.apiBaseUrl}/api/properties/10/photos/:photoId/content`,
        () => new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
      http.put(`${config.apiBaseUrl}/api/properties/10/photos/82/representative`, () => {
        representativeCalls += 1;
        representativePhotoId = 82;
        return new HttpResponse(null, { status: 200 });
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/photos');

    expect(await screen.findByRole('button', { name: '업로드 순 1번째 사진 대표 사진' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: '업로드 순 2번째 사진을 대표 사진으로 지정' }));

    expect(await screen.findByRole('button', { name: '업로드 순 2번째 사진 대표 사진' })).toBeDisabled();
    expect(representativeCalls).toBe(1);
  });

  it('사진 삭제 실패는 사진을 유지하고 안전한 재시도 오류를 표시한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailResponseFixture())),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ propertyId: 10, items: [propertyPhotoResponseFixture()], totalCount: 1 })),
      ),
      http.get(
        `${config.apiBaseUrl}/api/properties/10/photos/81/content`,
        () => new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
      http.delete(`${config.apiBaseUrl}/api/properties/10/photos/81`, () =>
        HttpResponse.json(errorEnvelope('PHOTO_DELETE_FAILED'), { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/photos');

    await user.click(await screen.findByRole('button', { name: '업로드 순 1번째 사진 삭제' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: '사진 삭제' }));
    expect(await within(dialog).findByText(/사진은 화면에서 제거하지 않았어요/)).toBeInTheDocument();
    expect(screen.getByRole('img', { name: '업로드 순 1번째 사진' })).toBeInTheDocument();
  });

  it('매물 삭제 영향·취소·성공을 처리하고 목록으로 이동한다', async () => {
    let deleteCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.delete(`${config.apiBaseUrl}/api/properties/10`, () => {
        deleteCalls += 1;
        return new HttpResponse(null, { status: 200 });
      }),
      http.get(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(successEnvelope(propertyPageFixture([]))),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10');

    await user.click(await screen.findByLabelText('매물 메뉴 열기'));
    await user.click(screen.getByRole('button', { name: '삭제' }));
    const dialog = screen.getByRole('dialog', { name: '신림역 원룸 매물을 삭제할까요?' });
    expect(within(dialog).getByText(/사진·메모·연결된 체크리스트도 함께 삭제/)).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: '취소' }));
    expect(deleteCalls).toBe(0);

    await user.click(screen.getByRole('button', { name: '삭제' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '매물 삭제' }));
    expect(await screen.findByRole('heading', { name: '내 매물' })).toHaveFocus();
    expect(deleteCalls).toBe(1);
  });

  it('PHOTO_DELETE_FAILED인 매물은 상세에 유지한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.delete(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(errorEnvelope('PHOTO_DELETE_FAILED'), { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10');

    await user.click(await screen.findByLabelText('매물 메뉴 열기'));
    await user.click(screen.getByRole('button', { name: '삭제' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: '매물 삭제' }));
    expect(await within(dialog).findByText(/매물은 그대로 유지/)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '신림역 원룸', level: 1 })).toBeInTheDocument();
  });

  it('PROPERTY_NOT_FOUND를 네트워크 오류와 구분해 목록 이동을 제공한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/99`, () =>
        HttpResponse.json(errorEnvelope('PROPERTY_NOT_FOUND'), { status: 404 }),
      ),
    );
    renderAuthenticated('/properties/99');

    expect(await screen.findByText('매물을 찾을 수 없어요.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '매물 목록으로 돌아가기' })).toHaveAttribute('href', '/properties');
    expect(screen.queryByRole('button', { name: '다시 시도' })).not.toBeInTheDocument();
  });
});
