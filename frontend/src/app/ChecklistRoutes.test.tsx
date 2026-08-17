import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import AppRoutes from './AppRoutes';
import { setAuthentication } from './authStore';
import { checklistQueryKeys } from './checklistQueryKeys';
import { queryClient } from './queryClient';
import {
  checkItemPageFixture,
  checklistDetailFixture,
  checklistPageFixture,
  checklistSummaryFixture,
  customChecklistItemFixture,
  mixedChecklistDetailFixture,
  onlineItemFixture,
  presetFixture,
  providedChecklistItemFixture,
  secondProvidedChecklistItemFixture,
  secondChecklistSummaryFixture,
  secondOnlineItemFixture,
} from '../test/checklistFixtures';
import { errorEnvelope, memberFixture, propertyDetailFixture, successEnvelope } from '../test/propertyFixtures';
import { server } from '../test/server';
import { visitDetailFixture } from '../test/visitFixtures';
import type { PublicConfig } from '../types/PublicConfig';
import type { ChecklistDetail } from '../types/Checklist';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  googleClientId: 'test-client',
  googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
};

type TestEntry = string | { pathname: string; state?: unknown };

const renderAuthenticated = (entry: TestEntry) => {
  setAuthentication({ accessToken: 'memory-token', tokenType: 'Bearer', expiresIn: 60 });
  server.use(http.get(`${config.apiBaseUrl}/api/members/me`, () => HttpResponse.json(successEnvelope(memberFixture))));
  return render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[entry]}>
          <AppRoutes config={config} storage={window.sessionStorage} />
        </MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  );
};

const useCatalogHandlers = () => {
  server.use(
    http.get(`${config.apiBaseUrl}/api/check-items`, () =>
      HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture, secondOnlineItemFixture]))),
    ),
    http.get(`${config.apiBaseUrl}/api/checklist-presets`, ({ request }) => {
      const presetType = new URL(request.url).searchParams.get('presetType');
      return HttpResponse.json(successEnvelope({ ...presetFixture, presetType }));
    }),
  );
};

describe('FE-3 체크리스트 탐색과 편집', () => {
  it('빈 단계에서는 안내 카드 대신 체크리스트 추가 동작을 제공한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(successEnvelope(checklistPageFixture([]))),
      ),
    );
    renderAuthenticated('/checklists/ONLINE_PHONE');

    expect(await screen.findByRole('link', { name: '체크리스트 추가' })).toHaveAttribute(
      'href',
      '/checklists/new?stage=ONLINE_PHONE',
    );
    expect(screen.queryByText('이 단계에 만든 체크리스트가 없어요.')).not.toBeInTheDocument();
  });

  it('체크리스트 진입 시 첫 단계 목록과 세 단계 탭을 표시하고 카탈로그를 미리 조회하지 않는다', async () => {
    let catalogCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/check-items`, () => {
        catalogCalls += 1;
        return HttpResponse.json(successEnvelope(checkItemPageFixture([])));
      }),
      http.get(`${config.apiBaseUrl}/api/checklist-presets`, () => {
        catalogCalls += 1;
        return HttpResponse.json(successEnvelope(presetFixture));
      }),
    );
    renderAuthenticated('/checklists');
    expect(await screen.findByRole('heading', { name: '내 체크리스트', level: 1 })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '온라인·전화' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: '집에서 확인' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '계약 전' })).toBeInTheDocument();
    expect(catalogCalls).toBe(0);
    expect(screen.getByRole('link', { name: '체크리스트' })).toHaveAttribute('aria-current', 'page');
  });

  it('같은 이름을 포함한 단계 목록을 모두 표시하고 편집 동작만 제공한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(
          successEnvelope(
            checklistPageFixture([
              checklistSummaryFixture,
              { ...secondChecklistSummaryFixture, name: checklistSummaryFixture.name },
            ]),
          ),
        ),
      ),
    );
    renderAuthenticated('/checklists/ONLINE_PHONE');
    expect(await screen.findAllByText('전화 문의 기본 목록')).toHaveLength(2);
    expect(screen.getAllByRole('link', { name: '편집' })).toHaveLength(2);
    expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument();
  });

  it('PROVIDED 항목만 재정렬해 생성하고 v1.1 전체 순서를 보낸다', async () => {
    useCatalogHandlers();
    let requestBody: unknown;
    server.use(
      http.post(`${config.apiBaseUrl}/api/checklists`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            ...checklistDetailFixture,
            checklistId: 9,
            name: '전화 확인',
          }),
          { status: 201 },
        );
      }),
      http.get(`${config.apiBaseUrl}/api/checklists/9`, () =>
        HttpResponse.json(successEnvelope({ ...checklistDetailFixture, checklistId: 9, name: '전화 확인' })),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE');
    await screen.findByRole('heading', { name: '새 체크리스트' });
    expect(screen.getAllByRole('link', { current: 'page' })).toHaveLength(2);
    await user.click(await screen.findByRole('button', { name: '원룸 제공 항목으로 시작' }));
    const name = await screen.findByLabelText('체크리스트 이름');
    await user.clear(name);
    await user.type(name, '  전화 확인  ');
    screen.getByRole('button', { name: `${secondOnlineItemFixture.question} 순서 변경` }).focus();
    await user.keyboard('{ArrowUp}');
    await user.click(screen.getByRole('button', { name: '체크리스트 만들기' }));
    expect(await screen.findByRole('heading', { name: '전화 확인', level: 1 })).toBeInTheDocument();
    expect(requestBody).toEqual({
      name: '전화 확인',
      stage: 'ONLINE_PHONE',
      items: [
        { origin: 'PROVIDED', sourceCheckItemId: 102 },
        { origin: 'PROVIDED', sourceCheckItemId: 101 },
      ],
    });
    expect(requestBody).not.toHaveProperty('checkItemIds');
  });

  it('시작 방식을 선택한 뒤에는 초기화 버튼을 노출하지 않는다', async () => {
    useCatalogHandlers();
    const user = userEvent.setup();
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE');
    await user.click(await screen.findByRole('button', { name: '빈 목록으로 시작' }));
    expect(await screen.findByLabelText('체크리스트 이름')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '시작 방식 변경 (항목 초기화)' })).not.toBeInTheDocument();
  });

  it('연결 화면에서 선택한 시작 방식으로 생성 편집기를 바로 연다', async () => {
    useCatalogHandlers();
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE&start=ONE_ROOM');

    expect(await screen.findByLabelText('체크리스트 이름')).toHaveValue('원룸 온라인·전화 체크리스트');
    expect(screen.queryByRole('heading', { name: '항목 구성 선택' })).not.toBeInTheDocument();
  });

  it('빈 목록 시작은 이전 프리셋 오류 상태와 무관하게 편집기를 연다', async () => {
    let presetCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture]))),
      ),
      http.get(`${config.apiBaseUrl}/api/checklist-presets`, () => {
        presetCalls += 1;
        return HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 });
      }),
    );

    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE&start=EMPTY');

    expect(await screen.findByLabelText('체크리스트 이름')).toHaveValue('온라인·전화 체크리스트');
    expect(screen.queryByText('프리셋을 불러오지 못했어요.')).not.toBeInTheDocument();
    expect(presetCalls).toBe(0);
  });

  it('프리셋 조회 실패 뒤 빈 목록으로 전환하면 오류 대신 편집기를 연다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture]))),
      ),
      http.get(`${config.apiBaseUrl}/api/checklist-presets`, () =>
        HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE');

    await user.click(await screen.findByRole('button', { name: '원룸 제공 항목으로 시작' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('프리셋을 불러오지 못했어요.');

    await user.click(screen.getByRole('button', { name: '빈 목록으로 시작' }));
    expect(await screen.findByLabelText('체크리스트 이름')).toHaveValue('온라인·전화 체크리스트');
    expect(screen.queryByText('프리셋을 불러오지 못했어요.')).not.toBeInTheDocument();
  });

  it('제공 항목 조회 실패는 편집 화면 안에서 재시도 동작을 제공한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 }),
      ),
    );
    renderAuthenticated('/checklists/new?stage=ON_SITE&start=EMPTY&mode=add-items');

    expect(await screen.findByRole('alert')).toHaveTextContent('요청을 처리하지 못했습니다');
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument();
  });

  it('CUSTOM 항목만으로 생성하며 신규 항목에 checklistItemId를 만들지 않는다', async () => {
    useCatalogHandlers();
    let requestBody: unknown;
    server.use(
      http.post(`${config.apiBaseUrl}/api/checklists`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            ...checklistDetailFixture,
            checklistId: 9,
            name: '나만의 질문',
            items: [customChecklistItemFixture],
            itemCount: 1,
          }),
          { status: 201 },
        );
      }),
      http.get(`${config.apiBaseUrl}/api/checklists/9`, () =>
        HttpResponse.json(
          successEnvelope({
            ...checklistDetailFixture,
            checklistId: 9,
            name: '나만의 질문',
            items: [customChecklistItemFixture],
            itemCount: 1,
          }),
        ),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE');
    await user.click(await screen.findByRole('button', { name: '빈 목록으로 시작' }));
    await user.clear(screen.getByLabelText('체크리스트 이름'));
    await user.type(screen.getByLabelText('체크리스트 이름'), '나만의 질문');
    await user.type(screen.getByLabelText('질문'), '  창틀 곰팡이는 괜찮은가?  ');
    await user.click(screen.getByRole('button', { name: '직접 질문 추가' }));
    expect(screen.getByLabelText('직접 추가 질문 1')).toHaveFocus();
    await user.click(screen.getByRole('button', { name: '체크리스트 만들기' }));
    await waitFor(() =>
      expect(requestBody).toEqual({
        name: '나만의 질문',
        stage: 'ONLINE_PHONE',
        items: [{ origin: 'CUSTOM', question: '창틀 곰팡이는 괜찮은가?' }],
      }),
    );
    expect(queryClient.getQueryData<ChecklistDetail>(checklistQueryKeys.detail(9))?.items[0]).toMatchObject({
      origin: 'CUSTOM',
      checklistItemId: 703,
    });
  });

  it('PROVIDED와 같은 문구 CUSTOM을 별개 항목으로 혼합 생성하고 순서를 보존한다', async () => {
    useCatalogHandlers();
    let requestBody: unknown;
    server.use(
      http.post(`${config.apiBaseUrl}/api/checklists`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(successEnvelope({ ...mixedChecklistDetailFixture, checklistId: 9 }), {
          status: 201,
        });
      }),
      http.get(`${config.apiBaseUrl}/api/checklists/9`, () =>
        HttpResponse.json(successEnvelope({ ...mixedChecklistDetailFixture, checklistId: 9 })),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE');
    await user.click(await screen.findByRole('button', { name: '원룸 제공 항목으로 시작' }));
    await user.type(await screen.findByLabelText('질문'), onlineItemFixture.question);
    await user.click(screen.getByRole('button', { name: '직접 질문 추가' }));
    screen
      .getAllByRole('button', { name: `${onlineItemFixture.question} 순서 변경` })
      .at(-1)!
      .focus();
    await user.keyboard('{ArrowUp}');
    await user.click(screen.getByRole('button', { name: '체크리스트 만들기' }));
    await waitFor(() =>
      expect(requestBody).toEqual({
        name: '원룸 온라인·전화 체크리스트',
        stage: 'ONLINE_PHONE',
        items: [
          { origin: 'PROVIDED', sourceCheckItemId: 101 },
          { origin: 'CUSTOM', question: onlineItemFixture.question },
          { origin: 'PROVIDED', sourceCheckItemId: 102 },
        ],
      }),
    );
  });

  it('상세 검색에 나오지 않는 기존 항목도 사용자가 제거하기 전까지 전체 교체 요청에 보존한다', async () => {
    const inactiveExistingItem = {
      ...providedChecklistItemFixture,
      checklistItemId: 799,
      sourceCheckItemId: 999,
      checkItemId: 999,
      question: '기존에만 남아 있는 항목',
    };
    let requestBody: unknown;
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(
          successEnvelope({
            ...checklistDetailFixture,
            items: [inactiveExistingItem, secondProvidedChecklistItemFixture],
            itemCount: 2,
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture, secondOnlineItemFixture]))),
      ),
      http.put(`${config.apiBaseUrl}/api/checklists/7`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            ...checklistDetailFixture,
            name: '보존 확인',
            items: [inactiveExistingItem, secondProvidedChecklistItemFixture],
            itemCount: 2,
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/7');
    const name = await screen.findByLabelText('체크리스트 이름');
    await user.clear(name);
    await user.type(name, '보존 확인');
    await user.click(screen.getByRole('button', { name: '변경 내용 저장' }));
    await waitFor(() =>
      expect(requestBody).toEqual({
        name: '보존 확인',
        items: [
          { origin: 'PROVIDED', sourceCheckItemId: 999 },
          { origin: 'PROVIDED', sourceCheckItemId: 102 },
        ],
      }),
    );
    expect(screen.getByText('기존에만 남아 있는 항목')).toBeInTheDocument();
    expect(screen.getByText(/더 이상 제공되지 않음/)).toBeInTheDocument();
  });

  it('기존 CUSTOM 질문을 빠짐없이 표시하고 수정 요청에 로컬 ID를 유지한다', async () => {
    let requestBody: unknown;
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(successEnvelope(mixedChecklistDetailFixture)),
      ),
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture, secondOnlineItemFixture]))),
      ),
      http.put(`${config.apiBaseUrl}/api/checklists/7`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            ...mixedChecklistDetailFixture,
            items: [
              providedChecklistItemFixture,
              { ...customChecklistItemFixture, question: '곰팡이 냄새는 괜찮은가?' },
            ],
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/7');
    const custom = await screen.findByLabelText('직접 추가 질문 2');
    expect(custom).toHaveValue(customChecklistItemFixture.question);
    await user.clear(custom);
    await user.type(custom, '곰팡이 냄새는 괜찮은가?');
    await user.click(screen.getByRole('button', { name: '변경 내용 저장' }));
    await waitFor(() =>
      expect(requestBody).toEqual({
        name: checklistDetailFixture.name,
        items: [
          { origin: 'PROVIDED', sourceCheckItemId: 101 },
          { origin: 'CUSTOM', checklistItemId: 703, question: '곰팡이 냄새는 괜찮은가?' },
        ],
      }),
    );
    expect(screen.getByLabelText('직접 추가 질문 2')).toHaveValue('곰팡이 냄새는 괜찮은가?');
    expect(queryClient.getQueryData<ChecklistDetail>(checklistQueryKeys.detail(7))?.items).toEqual([
      expect.objectContaining({ origin: 'PROVIDED', checklistItemId: 701, sourceCheckItemId: 101 }),
      expect.objectContaining({ origin: 'CUSTOM', checklistItemId: 703, question: '곰팡이 냄새는 괜찮은가?' }),
    ]);
  });

  it('동일 문구 CUSTOM을 두 번 추가할 수 있고 제공 항목 중복 선택은 막는다', async () => {
    useCatalogHandlers();
    const user = userEvent.setup();
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE');
    await user.click(await screen.findByRole('button', { name: '원룸 제공 항목으로 시작' }));
    const customInput = await screen.findByLabelText('질문');
    await user.type(customInput, '같은 질문');
    await user.click(screen.getByRole('button', { name: '직접 질문 추가' }));
    await user.type(customInput, '같은 질문');
    await user.click(screen.getByRole('button', { name: '직접 질문 추가' }));
    expect(screen.getByLabelText('직접 추가 질문 3')).toHaveValue('같은 질문');
    expect(screen.getByLabelText('직접 추가 질문 4')).toHaveValue('같은 질문');

    await user.click(screen.getByRole('button', { name: '+ 체크 항목 추가' }));
    expect(screen.getByText('체크 항목 편집')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '새 체크리스트로 돌아가기' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '취소' })).not.toBeInTheDocument();
    const providedCheckbox = await screen.findByRole('checkbox', { name: new RegExp(onlineItemFixture.question) });
    expect(providedCheckbox).toBeChecked();
    expect(providedCheckbox).toBeDisabled();
  });

  it('키보드로 혼합 항목 순서를 바꾸고 조작 버튼에 포커스를 유지한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(successEnvelope(mixedChecklistDetailFixture)),
      ),
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture]))),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/7');
    const moveHandle = await screen.findByRole('button', {
      name: `${customChecklistItemFixture.question} 순서 변경`,
    });
    moveHandle.focus();
    await user.keyboard('{ArrowUp}');
    const itemList = screen.getByRole('heading', { name: '확인 순서' }).closest('section')?.querySelector('ol');
    expect(itemList).not.toBeNull();
    expect(within(itemList as HTMLOListElement).getAllByRole('listitem')[0]).toHaveTextContent('직접 추가');
    expect(moveHandle).toHaveFocus();
  });

  it('마우스로 순서 조작 핸들을 끌어 혼합 항목 순서를 바꾼다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(successEnvelope(mixedChecklistDetailFixture)),
      ),
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture]))),
      ),
    );
    renderAuthenticated('/checklists/7');
    const moveHandle = await screen.findByRole('button', {
      name: `${customChecklistItemFixture.question} 순서 변경`,
    });
    const itemList = screen.getByRole('heading', { name: '확인 순서' }).closest('section')?.querySelector('ol');
    expect(itemList).not.toBeNull();
    const rows = within(itemList as HTMLOListElement).getAllByRole('listitem');
    const firstRow = rows[0];

    Object.defineProperties(moveHandle, {
      setPointerCapture: { configurable: true, value: vi.fn() },
      hasPointerCapture: { configurable: true, value: vi.fn(() => false) },
    });
    const originalElementFromPoint = document.elementFromPoint;
    Object.defineProperty(document, 'elementFromPoint', {
      configurable: true,
      value: vi.fn(() => firstRow),
    });

    try {
      fireEvent.pointerDown(moveHandle, {
        pointerId: 1,
        pointerType: 'mouse',
        isPrimary: true,
        button: 0,
        clientX: 0,
        clientY: 40,
      });
      fireEvent.pointerMove(moveHandle, {
        pointerId: 1,
        pointerType: 'mouse',
        isPrimary: true,
        button: 0,
        clientX: 0,
        clientY: 0,
      });
      fireEvent.pointerUp(moveHandle, {
        pointerId: 1,
        pointerType: 'mouse',
        isPrimary: true,
        button: 0,
        clientX: 0,
        clientY: 0,
      });
    } finally {
      Object.defineProperty(document, 'elementFromPoint', {
        configurable: true,
        value: originalElementFromPoint,
      });
    }

    expect(within(itemList as HTMLOListElement).getAllByRole('listitem')[0]).toHaveTextContent('직접 추가');
  });

  it('공백 및 200 코드포인트 초과 CUSTOM을 막고 이모지 200자는 허용한다', async () => {
    useCatalogHandlers();
    const user = userEvent.setup();
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE');
    await user.click(await screen.findByRole('button', { name: '빈 목록으로 시작' }));
    const customInput = screen.getByLabelText('질문');
    await user.type(customInput, '   ');
    await user.click(screen.getByRole('button', { name: '직접 질문 추가' }));
    expect(screen.getByText('직접 추가할 질문을 입력해 주세요.')).toBeInTheDocument();
    await user.clear(customInput);
    await user.type(customInput, '🏠'.repeat(201));
    expect(
      screen.getByText(
        (_, element) => element?.id === 'new-custom-question-help' && element.textContent?.includes('201/200') === true,
      ),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '직접 질문 추가' }));
    expect(screen.getByText('직접 추가 질문은 200자 이하로 입력해 주세요.')).toBeInTheDocument();
    await user.clear(customInput);
    await user.type(customInput, '🏠'.repeat(200));
    await user.click(screen.getByRole('button', { name: '직접 질문 추가' }));
    expect(screen.getByLabelText('직접 추가 질문 1')).toHaveValue('🏠'.repeat(200));
  });

  it('목록에 추가하지 않은 CUSTOM 입력을 저장이나 이동 중 조용히 버리지 않는다', async () => {
    useCatalogHandlers();
    let createCalls = 0;
    server.use(
      http.post(`${config.apiBaseUrl}/api/checklists`, () => {
        createCalls += 1;
        return HttpResponse.json(successEnvelope(mixedChecklistDetailFixture), { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/new?stage=ONLINE_PHONE');
    await user.click(await screen.findByRole('button', { name: '빈 목록으로 시작' }));
    const customInput = screen.getByLabelText('질문');
    await user.type(customInput, '아직 목록에 넣지 않은 질문');
    await user.click(screen.getByRole('button', { name: '체크리스트 만들기' }));
    expect(screen.getByText('입력 중인 직접 질문을 목록에 추가하거나 입력란을 비워 주세요.')).toBeInTheDocument();
    expect(customInput).toHaveFocus();
    expect(createCalls).toBe(0);
  });

  it('저장 실패와 상세 재조회에도 혼합 순서·이름·CUSTOM 초안을 유지하고 같은 버튼으로 재시도한다', async () => {
    let updateCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(successEnvelope(mixedChecklistDetailFixture)),
      ),
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture]))),
      ),
      http.put(`${config.apiBaseUrl}/api/checklists/7`, () => {
        updateCalls += 1;
        if (updateCalls === 1) return HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 });
        return HttpResponse.json(
          successEnvelope({
            ...mixedChecklistDetailFixture,
            name: '실패해도 남는 이름',
            items: [
              { ...customChecklistItemFixture, question: '수정 중인 질문', order: 1 },
              { ...providedChecklistItemFixture, order: 2 },
            ],
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/7');
    const name = await screen.findByLabelText('체크리스트 이름');
    const custom = screen.getByLabelText('직접 추가 질문 2');
    await user.clear(name);
    await user.type(name, '실패해도 남는 이름');
    await user.clear(custom);
    await user.type(custom, '수정 중인 질문');
    screen.getByRole('button', { name: '수정 중인 질문 순서 변경' }).focus();
    await user.keyboard('{ArrowUp}');
    await user.click(screen.getByRole('button', { name: '변경 내용 저장' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('작성한 내용은 그대로 유지');
    expect(name).toHaveValue('실패해도 남는 이름');
    expect(screen.getByLabelText('직접 추가 질문 1')).toHaveValue('수정 중인 질문');
    expect(updateCalls).toBe(1);
    await user.click(screen.getByRole('button', { name: '변경 내용 저장' }));
    await waitFor(() => expect(updateCalls).toBe(2));
    expect(await screen.findByText(/서버에서 확인한 최신 내용/)).toBeInTheDocument();
  });

  it('409 저장 오류는 자동 재시도하지 않고 안전한 문구와 초안을 유지한다', async () => {
    let updateCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(successEnvelope(mixedChecklistDetailFixture)),
      ),
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture]))),
      ),
      http.put(`${config.apiBaseUrl}/api/checklists/7`, () => {
        updateCalls += 1;
        return HttpResponse.json(errorEnvelope('CHECKLIST_REQUIRES_V11_CLIENT'), { status: 409 });
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/7');
    const name = await screen.findByLabelText('체크리스트 이름');
    await user.clear(name);
    await user.type(name, '409 초안');
    await user.click(screen.getByRole('button', { name: '변경 내용 저장' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('최신 편집 화면');
    expect(name).toHaveValue('409 초안');
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(updateCalls).toBe(1);
  });

  it('항목 제거 뒤 빈 목록 저장을 막고 가까운 오류를 표시한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(
          successEnvelope({ ...checklistDetailFixture, items: [providedChecklistItemFixture], itemCount: 1 }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture]))),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/7');
    await user.click(await screen.findByRole('button', { name: `${onlineItemFixture.question} 제거` }));
    expect(screen.getByLabelText('질문')).toHaveFocus();
    await user.click(screen.getByRole('button', { name: '변경 내용 저장' }));
    expect(screen.getByRole('alert')).toHaveTextContent('체크 항목을 한 개 이상 추가');
  });

  it('기존 삭제 확인 흐름을 유지하고 성공 뒤 상세 캐시와 목록을 갱신한다', async () => {
    let deleteCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(successEnvelope(mixedChecklistDetailFixture)),
      ),
      http.get(`${config.apiBaseUrl}/api/check-items`, () =>
        HttpResponse.json(successEnvelope(checkItemPageFixture([onlineItemFixture]))),
      ),
      http.delete(`${config.apiBaseUrl}/api/checklists/7`, () => {
        deleteCalls += 1;
        return new HttpResponse(null, { status: 204 });
      }),
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(successEnvelope(checklistPageFixture([]))),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/checklists/7');
    await user.click(await screen.findByRole('button', { name: '체크리스트 삭제' }));
    expect(screen.getByRole('dialog')).toHaveTextContent('완료한 방문 기록의 스냅샷은 유지');
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '체크리스트 삭제' }));
    expect(await screen.findByRole('heading', { name: '내 체크리스트', level: 1 })).toBeInTheDocument();
    expect(deleteCalls).toBe(1);
    expect(queryClient.getQueryData(checklistQueryKeys.detail(7))).toBeUndefined();
  });

  it('찾을 수 없는 체크리스트는 편집 폼 대신 안전한 안내를 표시한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/404`, () =>
        HttpResponse.json(errorEnvelope('CHECKLIST_NOT_FOUND'), { status: 404 }),
      ),
    );
    renderAuthenticated('/checklists/404');
    expect(await screen.findByRole('alert')).toHaveTextContent('체크리스트를 찾을 수 없어요.');
    expect(screen.queryByLabelText('체크리스트 이름')).not.toBeInTheDocument();
  });

  it('편집 중 내부 링크 이동 전에 미저장 변경을 경고하고 취소하면 초안을 유지한다', async () => {
    useCatalogHandlers();
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(successEnvelope(checklistDetailFixture)),
      ),
    );
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const user = userEvent.setup();
    renderAuthenticated('/checklists/7');
    const name = await screen.findByLabelText('체크리스트 이름');
    await user.clear(name);
    await user.type(name, '저장하지 않은 이름');
    await user.click(screen.getByRole('link', { name: '체크리스트 목록으로 돌아가기' }));
    expect(confirm).toHaveBeenCalledOnce();
    expect(screen.getByLabelText('체크리스트 이름')).toHaveValue('저장하지 않은 이름');
  });
});

describe('FE-3 매물 활성 체크리스트', () => {
  it('연결할 목록이 없으면 연결 화면에서 시작 구성을 바로 선택한다', async () => {
    useCatalogHandlers();
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(
          successEnvelope({
            ...propertyDetailFixture,
            activeChecklists: [],
            photoPreview: { totalCount: 0, photos: [] },
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(successEnvelope(checklistPageFixture([]))),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/active-checklists/ONLINE_PHONE');

    expect(await screen.findByRole('button', { name: '빈 목록으로 시작' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '원룸 제공 항목으로 시작' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '새로 만들기' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '원룸 제공 항목으로 시작' }));
    expect(await screen.findByLabelText('체크리스트 이름')).toHaveValue('원룸 온라인·전화 체크리스트');
  });

  it('진행 중 방문이 있으면 연결된 단계에서 해당 체크 화면으로 바로 이동한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(
          successEnvelope({
            ...propertyDetailFixture,
            activeChecklists: [{ stage: 'ONLINE_PHONE', checklistId: 7, name: '전화 문의 기본 목록', itemCount: 2 }],
            recentVisit: {
              ...propertyDetailFixture.recentVisit,
              status: 'IN_PROGRESS',
              completedAt: null,
            },
            photoPreview: { totalCount: 0, photos: [] },
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/visits/31`, () =>
        HttpResponse.json(successEnvelope({ ...visitDetailFixture, status: 'IN_PROGRESS', completedAt: null })),
      ),
    );
    renderAuthenticated('/properties/10');
    expect(await screen.findByRole('link', { name: '온라인·전화 체크하기' })).toHaveAttribute(
      'href',
      '/visits/31?stage=ONLINE_PHONE',
    );
    expect(screen.getByRole('link', { name: '계약 전 체크리스트 연결' }).parentElement).toHaveClass(
      /unlinkedChecklist/,
    );
  });

  it('매물 상세에서 세 단계와 최근 체크 결과를 한 번에 확인한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope({ ...propertyDetailFixture, photoPreview: { totalCount: 0, photos: [] } })),
      ),
      http.get(`${config.apiBaseUrl}/api/visits/31`, () => HttpResponse.json(successEnvelope(visitDetailFixture))),
    );
    renderAuthenticated('/properties/10');
    const section = await screen.findByRole('heading', { name: '체크리스트' });
    const container = section.closest('section');
    expect(container).not.toBeNull();
    expect(within(container as HTMLElement).getByRole('link', { name: '연결 관리' })).toHaveAttribute(
      'href',
      '/properties/10/active-checklists/ONLINE_PHONE',
    );
    expect(within(container as HTMLElement).getAllByRole('tab')).toHaveLength(3);
    expect(await within(container as HTMLElement).findByText('관리비 포함 항목을 확인했나요?')).toBeInTheDocument();
    expect(within(container as HTMLElement).getAllByText('연결된 체크리스트 없음')).toHaveLength(1);
    expect(within(container as HTMLElement).getByText('밤 소음도 다시 확인')).toBeInTheDocument();
    expect(within(container as HTMLElement).getByRole('link', { name: '온라인·전화 체크하기' })).toHaveAttribute(
      'href',
      '/visits/31?stage=ONLINE_PHONE',
    );
    expect(within(container as HTMLElement).getByRole('link', { name: '집에서 확인 체크하기' })).toHaveAttribute(
      'href',
      '/visits/31?stage=ON_SITE',
    );
    expect(within(container as HTMLElement).getByRole('link', { name: '계약 전 체크리스트 연결' })).toHaveAttribute(
      'href',
      '/properties/10/active-checklists/PRE_CONTRACT',
    );
  });

  it('목록 선택만으로는 API-401을 호출하지 않고 최종 확인 때 연결한다', async () => {
    let assignCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(
          successEnvelope({
            ...propertyDetailFixture,
            activeChecklists: [],
            photoPreview: { totalCount: 0, photos: [] },
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(
          successEnvelope(checklistPageFixture([checklistSummaryFixture, secondChecklistSummaryFixture])),
        ),
      ),
      http.put(`${config.apiBaseUrl}/api/properties/10/active-checklists/ONLINE_PHONE`, async ({ request }) => {
        assignCalls += 1;
        expect(await request.json()).toEqual({ checklistId: 8 });
        return HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            stage: 'ONLINE_PHONE',
            checklistId: 8,
            name: secondChecklistSummaryFixture.name,
            itemCount: 2,
          }),
        );
      }),
      http.post(`${config.apiBaseUrl}/api/properties/10/visits`, () =>
        HttpResponse.json(successEnvelope(visitDetailFixture), { status: 201 }),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/active-checklists/ONLINE_PHONE');
    const checklist = await screen.findByRole('checkbox', { name: /직방 매물 문의 목록/ });
    expect(screen.queryByRole('button', { name: '이 체크리스트 연결' })).not.toBeInTheDocument();
    await user.click(checklist);
    expect(assignCalls).toBe(0);
    await user.click(screen.getByRole('button', { name: '이 체크리스트 연결' }));
    expect(
      await screen.findByRole('heading', { name: `${propertyDetailFixture.name} 방문`, level: 1 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /온라인·전화/ })).toHaveAttribute('aria-selected', 'true');
    expect(assignCalls).toBe(1);
  });

  it('생성 후 돌아온 체크리스트는 선택만 하고 최종 확인 전까지 연결하지 않는다', async () => {
    let assignCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(
          successEnvelope({
            ...propertyDetailFixture,
            activeChecklists: [],
            photoPreview: { totalCount: 0, photos: [] },
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(
          successEnvelope(checklistPageFixture([checklistSummaryFixture, secondChecklistSummaryFixture])),
        ),
      ),
      http.put(`${config.apiBaseUrl}/api/properties/10/active-checklists/ONLINE_PHONE`, () => {
        assignCalls += 1;
        return HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            stage: 'ONLINE_PHONE',
            checklistId: 8,
            name: secondChecklistSummaryFixture.name,
            itemCount: 2,
          }),
        );
      }),
    );
    renderAuthenticated({ pathname: '/properties/10/active-checklists/ONLINE_PHONE', state: { newChecklistId: 8 } });
    expect(await screen.findByRole('checkbox', { name: /직방 매물 문의 목록/ })).toBeChecked();
    expect(screen.getByText('방금 생성')).toBeInTheDocument();
    expect(assignCalls).toBe(0);
  });

  it('현재 연결된 체크리스트를 다시 누르면 별도 해제 버튼 없이 연결을 해제한다', async () => {
    let connected = true;
    let removeCalls = 0;
    const onSiteChecklist = {
      ...checklistSummaryFixture,
      name: '집에서 확인할 목록',
      stage: 'ON_SITE',
    };
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(
          successEnvelope({
            ...propertyDetailFixture,
            activeChecklists: connected
              ? [{ stage: 'ON_SITE', checklistId: 7, name: onSiteChecklist.name, itemCount: 2 }]
              : [],
            photoPreview: { totalCount: 0, photos: [] },
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/checklists`, () =>
        HttpResponse.json(successEnvelope(checklistPageFixture([onSiteChecklist]))),
      ),
      http.delete(`${config.apiBaseUrl}/api/properties/10/active-checklists/ON_SITE`, () => {
        connected = false;
        removeCalls += 1;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/active-checklists/ON_SITE');
    const checkbox = await screen.findByRole('checkbox', { name: /집에서 확인할 목록/ });
    expect(checkbox).toBeChecked();
    expect(screen.queryByRole('button', { name: '이 단계 연결 해제' })).not.toBeInTheDocument();

    await user.click(checkbox);

    await waitFor(() => expect(removeCalls).toBe(1));
    await waitFor(() => expect(checkbox).not.toBeChecked());
  });
});
