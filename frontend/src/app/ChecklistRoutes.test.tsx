import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import {
  checkItemPageFixture,
  checklistPageFixture,
  checklistSummaryFixture,
  onlineItemFixture,
  secondChecklistSummaryFixture,
  secondOnlineItemFixture,
} from '../test/checklistFixtures';
import { propertyDetailResponseFixture, successEnvelope } from '../test/propertyFixtures';
import { server } from '../test/server';
import type { PublicConfig } from '../types/PublicConfig';
import AppRoutes from './AppRoutes';
import { setAuthentication } from './authStore';
import { queryClient } from './queryClient';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
};

type TestEntry = string | { pathname: string; state?: unknown };

const renderAuthenticated = (entry: TestEntry) => {
  setAuthentication({ accessToken: 'memory-token', tokenType: 'Bearer', expiresIn: 60 });
  return render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[entry]}>
          <AppRoutes config={config} />
        </MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  );
};

const finalChecklistDetail = (overrides: Record<string, unknown> = {}) => ({
  id: 7,
  name: '전화 문의 기본 목록',
  stage: 'ONLINE_PHONE',
  itemCount: 2,
  items: [
    {
      id: 701,
      origin: 'PROVIDED',
      systemCheckItemId: 101,
      itemType: 'CORE',
      question: onlineItemFixture.question,
      displayOrder: 1,
      active: true,
    },
    {
      id: 702,
      origin: 'PROVIDED',
      systemCheckItemId: 102,
      itemType: 'OPTIONAL',
      question: secondOnlineItemFixture.question,
      displayOrder: 2,
      active: true,
    },
  ],
  ...overrides,
});

const emptyStageProgress = (stage: 'ON_SITE' | 'PRE_CONTRACT') => ({
  stage,
  applied: false,
  propertyChecklistId: null,
  checklistName: null,
  sourceChecklistId: null,
  progress: {
    totalCount: 0,
    completedCount: 0,
    goodCount: 0,
    cautionCount: 0,
    unconfirmedCount: 0,
    progressRate: 0,
  },
});

describe('체크리스트 탐색과 편집', () => {
  it('체크리스트 홈에서 세 단계를 안내하고 단계 목록으로 이동한다', async () => {
    renderAuthenticated('/checklists');

    expect(await screen.findByRole('heading', { name: '체크리스트', level: 1 })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /온라인·전화 확인/ })).toHaveAttribute('href', '/checklists/ONLINE_PHONE');
    expect(screen.getByRole('link', { name: /집에서 확인/ })).toHaveAttribute('href', '/checklists/ON_SITE');
    expect(screen.getByRole('link', { name: /부동산 계약 확인/ })).toHaveAttribute('href', '/checklists/PRE_CONTRACT');
    expect(screen.getByRole('link', { name: '체크리스트' })).toHaveAttribute('aria-current', 'page');
  });

  it('빈 단계에서는 별도 빈 카드 없이 새 체크리스트 만들기 링크를 제공한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(successEnvelope(checklistPageFixture([]))),
      ),
    );
    renderAuthenticated('/checklists/ONLINE_PHONE');

    expect(await screen.findByRole('link', { name: '새 체크리스트 만들기' })).toHaveAttribute(
      'href',
      '/checklists/new?stage=ONLINE_PHONE',
    );
    expect(screen.queryByText('이 단계에 만든 체크리스트가 없어요.')).not.toBeInTheDocument();
  });

  it('단계 목록에 편집과 삭제 동작을 함께 표시한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(
          successEnvelope(checklistPageFixture([checklistSummaryFixture, secondChecklistSummaryFixture])),
        ),
      ),
    );
    renderAuthenticated('/checklists/ONLINE_PHONE');

    expect(await screen.findByRole('link', { name: '전화 문의 기본 목록 편집' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '전화 문의 기본 목록 삭제' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '직방 매물 문의 목록 편집' })).toBeInTheDocument();
  });

  it('새 체크리스트는 CORE로 열리고 제공 OPTIONAL과 직접 질문을 함께 추가한다', async () => {
    let requestBody: unknown;
    server.use(
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture, secondOnlineItemFixture]))),
      ),
      http.post(`${config.apiBaseUrl}/api/checklists`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope(finalChecklistDetail({ id: 9, name: '원룸 온라인·전화 체크리스트' })),
          { status: 201 },
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE');

    expect(await screen.findByLabelText('체크리스트 이름')).toHaveValue('원룸 온라인·전화 체크리스트');
    expect(screen.getByText(onlineItemFixture.question)).toBeInTheDocument();
    expect(screen.queryByText(secondOnlineItemFixture.question)).not.toBeInTheDocument();
    expect(screen.queryByText('빈 목록')).not.toBeInTheDocument();
    expect(screen.queryByText('원룸 제공 항목')).not.toBeInTheDocument();

    const addItemButton = screen.getByRole('button', { name: '+ 체크 항목 추가' });
    const orderHeading = screen.getByRole('heading', { name: '확인 순서' });
    expect(addItemButton.compareDocumentPosition(orderHeading) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();

    await user.click(addItemButton);
    const optionalItem = await screen.findByRole('checkbox', { name: secondOnlineItemFixture.question });
    expect(optionalItem).not.toBeChecked();
    await user.click(optionalItem);
    await user.type(screen.getByLabelText('내 질문 직접 추가'), '창틀 곰팡이는 괜찮은가?');
    await user.click(screen.getByRole('button', { name: '선택한 2개 항목 추가' }));
    expect(screen.getByText(secondOnlineItemFixture.question)).toBeInTheDocument();
    expect(screen.getByLabelText('직접 추가 질문')).toHaveValue('창틀 곰팡이는 괜찮은가?');

    await user.click(screen.getByRole('button', { name: '체크리스트 만들기' }));
    await waitFor(() =>
      expect(requestBody).toEqual({
        name: '원룸 온라인·전화 체크리스트',
        stage: 'ONLINE_PHONE',
        items: [
          { systemCheckItemId: 101 },
          { systemCheckItemId: 102 },
          { systemCheckItemId: null, question: '창틀 곰팡이는 괜찮은가?' },
        ],
      }),
    );
    expect(await screen.findByRole('link', { name: '새 체크리스트 만들기' })).toHaveAttribute(
      'href',
      '/checklists/new?stage=ONLINE_PHONE',
    );
  });

  it('원룸 제공 항목을 불러오지 못하면 빈 목록으로 우회하지 않고 재시도한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json({ code: 'INTERNAL_SERVER_ERROR', message: '조회 실패', data: null }, { status: 503 }),
      ),
    );
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE');

    const error = await screen.findByRole('alert');
    expect(within(error).getByText('프리셋을 불러오지 못했어요.')).toBeInTheDocument();
    expect(within(error).getByRole('button', { name: '다시 시도' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '빈 목록으로 시작' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText('체크리스트 이름')).not.toBeInTheDocument();
  });

  it('수정 요청에는 현재 제공 항목의 전체 순서를 보낸다', async () => {
    let requestBody: unknown;
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(successEnvelope(finalChecklistDetail())),
      ),
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture, secondOnlineItemFixture]))),
      ),
      http.put(`${config.apiBaseUrl}/api/checklists/7`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(successEnvelope(finalChecklistDetail({ name: '수정한 목록' })));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/7');

    const name = await screen.findByLabelText('체크리스트 이름');
    await user.clear(name);
    await user.type(name, '수정한 목록');
    await user.click(screen.getByRole('button', { name: '변경 내용 저장' }));

    await waitFor(() =>
      expect(requestBody).toEqual({
        name: '수정한 목록',
        items: [{ systemCheckItemId: 101 }, { systemCheckItemId: 102 }],
      }),
    );
    expect(await screen.findByRole('link', { name: '새 체크리스트 만들기' })).toHaveAttribute(
      'href',
      '/checklists/new?stage=ONLINE_PHONE',
    );
  });
});

describe('매물 체크리스트 연결과 자동 저장', () => {
  it('매물의 모든 단계는 체크리스트 적용 여부와 관계없이 선택 화면으로 이동한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope({ ...propertyDetailResponseFixture(), photos: [] })),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/checklists`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            overallProgress: {
              totalCount: 2,
              completedCount: 1,
              goodCount: 1,
              cautionCount: 0,
              unconfirmedCount: 1,
              progressRate: 50,
            },
            stages: [
              {
                stage: 'ONLINE_PHONE',
                applied: true,
                propertyChecklistId: 47,
                checklistName: '전화 문의 기본 목록',
                sourceChecklistId: 7,
                progress: {
                  totalCount: 2,
                  completedCount: 1,
                  goodCount: 1,
                  cautionCount: 0,
                  unconfirmedCount: 1,
                  progressRate: 50,
                },
              },
              emptyStageProgress('ON_SITE'),
              emptyStageProgress('PRE_CONTRACT'),
            ],
          }),
        ),
      ),
    );
    renderAuthenticated('/properties/10');

    const sectionHeading = await screen.findByRole('heading', { name: '3단계 체크리스트' });
    const section = sectionHeading.closest('section');
    expect(section).not.toBeNull();
    expect(
      within(section as HTMLElement).getByRole('link', { name: /온라인·전화.*전화 문의 기본 목록/ }),
    ).toHaveAttribute('href', '/properties/10/active-checklists/ONLINE_PHONE?from=property-detail&mode=replace');
    const onlineStage = within(section as HTMLElement).getByRole('link', {
      name: /온라인·전화.*전화 문의 기본 목록/,
    });
    expect(within(onlineStage).getByLabelText(/\uC9C4\uD589 1\/2/)).toBeInTheDocument();
    expect(within(onlineStage).getByRole('list')).toHaveTextContent(/1.*0.*1/);
    expect(
      within(section as HTMLElement).getByRole('link', { name: /계약 전.*연결된 체크리스트 없음/ }),
    ).toHaveAttribute('href', '/properties/10/active-checklists/PRE_CONTRACT?from=property-detail');
    const preContractStage = within(section as HTMLElement).getByRole('link', {
      name: /계약 전.*연결된 체크리스트 없음/,
    });
    expect(within(preContractStage).getByLabelText(/\uC9C4\uD589 0\/0/)).toBeInTheDocument();
  });

  it('내 체크리스트가 없어도 시작 방식 선택 없이 새 체크리스트 만들기만 제공한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailResponseFixture())),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/checklists`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            overallProgress: {
              totalCount: 0,
              completedCount: 0,
              goodCount: 0,
              cautionCount: 0,
              unconfirmedCount: 0,
              progressRate: 0,
            },
            stages: [
              {
                stage: 'ONLINE_PHONE',
                applied: false,
                propertyChecklistId: null,
                checklistName: null,
                sourceChecklistId: null,
                progress: {
                  totalCount: 0,
                  completedCount: 0,
                  goodCount: 0,
                  cautionCount: 0,
                  unconfirmedCount: 0,
                  progressRate: 0,
                },
              },
              emptyStageProgress('ON_SITE'),
              emptyStageProgress('PRE_CONTRACT'),
            ],
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(successEnvelope(checklistPageFixture([]))),
      ),
    );
    renderAuthenticated('/properties/10/active-checklists/ONLINE_PHONE?from=property-detail');

    expect(await screen.findByRole('checkbox', { name: /자취선배 기본 체크리스트/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '새 체크리스트 만들기' })).toHaveAttribute(
      'href',
      '/checklists/new?stage=ONLINE_PHONE&returnTo=%2Fproperties%2F10%2Factive-checklists%2FONLINE_PHONE%3Ffrom%3Dproperty-detail',
    );
    expect(screen.queryByText('빈 목록')).not.toBeInTheDocument();
    expect(screen.queryByText('원룸 제공 항목')).not.toBeInTheDocument();
  });

  it('목록을 고른 뒤 확인할 때 최종 연결 API를 호출하고 적용 상세로 이동한다', async () => {
    let requestBody: unknown;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailResponseFixture())),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/checklists`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            overallProgress: {
              totalCount: 0,
              completedCount: 0,
              goodCount: 0,
              cautionCount: 0,
              unconfirmedCount: 0,
              progressRate: 0,
            },
            stages: [
              {
                stage: 'ONLINE_PHONE',
                applied: false,
                propertyChecklistId: null,
                checklistName: null,
                sourceChecklistId: null,
                progress: {
                  totalCount: 0,
                  completedCount: 0,
                  goodCount: 0,
                  cautionCount: 0,
                  unconfirmedCount: 0,
                  progressRate: 0,
                },
              },
              emptyStageProgress('ON_SITE'),
              emptyStageProgress('PRE_CONTRACT'),
            ],
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(successEnvelope(checklistPageFixture([checklistSummaryFixture]))),
      ),
      http.put(`${config.apiBaseUrl}/api/properties/10/checklists/ONLINE_PHONE`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            id: 47,
            propertyId: 10,
            sourceChecklistId: 7,
            checklistName: '전화 문의 기본 목록',
            stage: 'ONLINE_PHONE',
            items: [],
          }),
        );
      }),
      http.get(`${config.apiBaseUrl}/api/properties/10/checklists/47`, () =>
        HttpResponse.json(
          successEnvelope({
            id: 47,
            propertyId: 10,
            sourceChecklistId: 7,
            checklistName: '전화 문의 기본 목록',
            stage: 'ONLINE_PHONE',
            items: [],
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/checklists`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            overallProgress: {
              totalCount: 2,
              completedCount: 1,
              goodCount: 1,
              cautionCount: 0,
              unconfirmedCount: 1,
              progressRate: 50,
            },
            stages: [
              {
                stage: 'ONLINE_PHONE',
                applied: true,
                propertyChecklistId: 47,
                checklistName: '전화 문의 기본 목록',
                sourceChecklistId: 7,
                progress: {
                  totalCount: 1,
                  completedCount: 0,
                  goodCount: 0,
                  cautionCount: 0,
                  unconfirmedCount: 1,
                  progressRate: 0,
                },
              },
              emptyStageProgress('ON_SITE'),
              emptyStageProgress('PRE_CONTRACT'),
            ],
          }),
        ),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/active-checklists/ONLINE_PHONE?from=property-detail');

    expect(await screen.findByRole('checkbox', { name: /전화 문의 기본 목록/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '온라인·전화' })).toBeInTheDocument();

    await user.click(screen.getByRole('checkbox', { name: /전화 문의 기본 목록/ }));
    expect(requestBody).toBeUndefined();
    await user.click(screen.getByRole('button', { name: '이 체크리스트 연결' }));

    await waitFor(() => expect(requestBody).toEqual({ sourceType: 'USER', checklistId: 7 }));
    expect(await screen.findByRole('heading', { name: '전화 문의 기본 목록', level: 1 })).toBeInTheDocument();
  });

  it('연결을 확정하지 않고 단계를 바꾸면 선택 상태를 폐기한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailResponseFixture())),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/checklists`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            overallProgress: {
              totalCount: 0,
              completedCount: 0,
              goodCount: 0,
              cautionCount: 0,
              unconfirmedCount: 0,
              progressRate: 0,
            },
            stages: [
              {
                stage: 'ONLINE_PHONE',
                applied: false,
                propertyChecklistId: null,
                checklistName: null,
                sourceChecklistId: null,
                progress: {
                  totalCount: 0,
                  completedCount: 0,
                  goodCount: 0,
                  cautionCount: 0,
                  unconfirmedCount: 0,
                  progressRate: 0,
                },
              },
              emptyStageProgress('ON_SITE'),
              emptyStageProgress('PRE_CONTRACT'),
            ],
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/checklists`, ({ request }) => {
        const stage = new URL(request.url).searchParams.get('stage');
        const item = {
          ...checklistSummaryFixture,
          id: stage === 'ON_SITE' ? 9 : 7,
          checklistId: stage === 'ON_SITE' ? 9 : 7,
          name: stage === 'ON_SITE' ? '집에서 확인할 목록' : '전화 문의 기본 목록',
          stage,
        };
        return HttpResponse.json(successEnvelope(checklistPageFixture([item])));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/active-checklists/ONLINE_PHONE');

    await user.click(await screen.findByRole('checkbox', { name: /전화 문의 기본 목록/ }));
    expect(screen.getByRole('button', { name: '이 체크리스트 연결' })).toBeEnabled();

    await user.click(screen.getByRole('link', { name: '집에서 확인' }));

    expect(await screen.findByRole('checkbox', { name: /집에서 확인할 목록/ })).not.toBeChecked();
    expect(screen.queryByRole('button', { name: '이 체크리스트 연결' })).not.toBeInTheDocument();
  });

  it('적용된 체크리스트에서 변경을 누르면 현재 연결을 유지한 채 교체 목록을 보여준다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailResponseFixture())),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/checklists`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            overallProgress: {
              totalCount: 2,
              completedCount: 1,
              goodCount: 1,
              cautionCount: 0,
              unconfirmedCount: 1,
              progressRate: 50,
            },
            stages: [
              {
                stage: 'ONLINE_PHONE',
                applied: true,
                propertyChecklistId: 47,
                checklistName: '전화 문의 기본 목록',
                sourceChecklistId: 7,
                progress: {
                  totalCount: 2,
                  completedCount: 1,
                  goodCount: 1,
                  cautionCount: 0,
                  unconfirmedCount: 1,
                  progressRate: 50,
                },
              },
              emptyStageProgress('ON_SITE'),
              emptyStageProgress('PRE_CONTRACT'),
            ],
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(
          successEnvelope(checklistPageFixture([checklistSummaryFixture, secondChecklistSummaryFixture])),
        ),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/active-checklists/ONLINE_PHONE?from=property-detail&mode=replace');

    const current = await screen.findByRole('checkbox', { name: /전화 문의 기본 목록/ });
    expect(current).toBeChecked();
    expect(screen.getByRole('checkbox', { name: /직방 매물 문의 목록/ })).not.toBeChecked();

    await user.click(screen.getByRole('checkbox', { name: /직방 매물 문의 목록/ }));
    expect(screen.getByRole('button', { name: '선택한 체크리스트로 교체' })).toBeEnabled();
  });

  it('상태는 선택 즉시, 메모는 포커스가 빠질 때 자동 저장한다', async () => {
    let statusRequest: unknown;
    let memoRequest: unknown;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope({ ...propertyDetailResponseFixture(), photos: [] })),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/checklists/47`, () =>
        HttpResponse.json(
          successEnvelope({
            id: 47,
            propertyId: 10,
            sourceChecklistId: 7,
            checklistName: '전화 문의 기본 목록',
            stage: 'ONLINE_PHONE',
            items: [
              {
                id: 701,
                systemCheckItemId: 101,
                question: '수압이 충분한가요?',
                displayOrder: 1,
                status: 'UNCONFIRMED',
                memo: '',
              },
            ],
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/checklists`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            overallProgress: {
              totalCount: 1,
              completedCount: 0,
              goodCount: 0,
              cautionCount: 0,
              unconfirmedCount: 1,
              progressRate: 0,
            },
            stages: [
              {
                stage: 'ONLINE_PHONE',
                applied: true,
                propertyChecklistId: 47,
                checklistName: '전화 문의 기본 목록',
                sourceChecklistId: 7,
                progress: {
                  totalCount: 1,
                  completedCount: 0,
                  goodCount: 0,
                  cautionCount: 0,
                  unconfirmedCount: 1,
                  progressRate: 0,
                },
              },
              emptyStageProgress('ON_SITE'),
              emptyStageProgress('PRE_CONTRACT'),
            ],
          }),
        ),
      ),
      http.patch(`${config.apiBaseUrl}/api/properties/10/checklists/47/items/701/status`, async ({ request }) => {
        statusRequest = await request.json();
        return HttpResponse.json(successEnvelope({ item: { id: 701, status: 'CAUTION' } }));
      }),
      http.patch(`${config.apiBaseUrl}/api/properties/10/checklists/47/items/701/memo`, async ({ request }) => {
        memoRequest = await request.json();
        return HttpResponse.json(successEnvelope({ item: { id: 701, memo: '욕실도 확인' } }));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/checklists/47');

    expect(await screen.findByRole('heading', { name: '전화 문의 기본 목록', level: 1 })).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: '온라인·전화' })).toHaveAttribute(
      'href',
      '/properties/10/checklists/47',
    );
    expect(screen.getByRole('link', { name: '집에서 확인' })).toHaveAttribute(
      'href',
      '/properties/10/active-checklists/ON_SITE?from=property-detail',
    );
    expect(screen.queryByRole('link', { name: '체크리스트 변경' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('radio', { name: '주의' }));
    await waitFor(() => expect(statusRequest).toEqual({ status: 'CAUTION' }));

    await user.click(screen.getByRole('button', { name: '수압이 충분한가요? 메모 열기' }));
    const memo = screen.getByRole('textbox', { name: '수압이 충분한가요? 메모' });
    await user.type(memo, '욕실도 확인');
    expect(memoRequest).toBeUndefined();
    await user.tab();
    await waitFor(() => expect(memoRequest).toEqual({ memo: '욕실도 확인' }));
  });
});
