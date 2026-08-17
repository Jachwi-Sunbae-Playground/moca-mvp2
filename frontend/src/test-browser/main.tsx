import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';
import { clearAuthentication, setAuthentication } from '../app/authStore';
import {
  checkItemPageFixture,
  checklistPageFixture,
  checklistSummaryFixture,
  customChecklistItemFixture,
  mixedChecklistDetailFixture,
  onlineItemFixture,
  presetFixture,
  providedChecklistItemFixture,
  secondChecklistSummaryFixture,
} from '../test/checklistFixtures';
import {
  errorEnvelope,
  memberFixture,
  photoFixture,
  propertyDetailFixture,
  propertyPageFixture,
  propertySummaryFixture,
  secondPropertySummaryFixture,
  successEnvelope,
} from '../test/propertyFixtures';
import { visitDetailFixture, visitPageFixture } from '../test/visitFixtures';
import type { ChecklistDetail } from '../types/Checklist';
import ComponentCatalog from './ComponentCatalog';

const scenario = new URLSearchParams(window.location.search).get('scenario') ?? 'list-two';

const routeByScenario: Record<string, string> = {
  components: '/',
  login: '/login',
  unauthorized: '/properties',
  'not-found': '/does-not-exist',
  'list-empty': '/properties',
  'list-two': '/properties',
  new: '/properties/new',
  detail: '/properties/10',
  'memo-empty': '/properties/10',
  'memo-success': '/properties/10',
  'memo-failure': '/properties/10',
  'property-delete': '/properties/10',
  'photos-empty': '/properties/10/photos',
  'photos-many': '/properties/10/photos',
  'photo-read-failure': '/properties/10/photos',
  'photo-delete': '/properties/10/photos',
  'checklist-home': '/checklists',
  'checklist-list': '/checklists/ONLINE_PHONE',
  'checklist-create': '/checklists/new?stage=ONLINE_PHONE',
  'checklist-detail': '/checklists/7',
  'checklist-detail-inactive': '/checklists/7',
  'checklist-detail-many': '/checklists/7',
  'checklist-save-failure': '/checklists/7',
  'active-checklist': '/properties/10/active-checklists/ONLINE_PHONE',
  'visit-list': '/properties/10/visits',
  'visit-detail': '/visits/31',
  'visit-completed': '/visits/31',
  'visit-conflict': '/visits/31',
  'visit-memo-conflict': '/visits/31',
  'visit-save-failure': '/visits/31',
  'visit-completion-failure': '/visits/31',
  my: '/me',
  'todo-compare': '/compare',
  'todo-export': '/export',
  'todo-tips': '/tips',
};

window.history.replaceState(null, '', routeByScenario[scenario] ?? '/properties');
if (scenario === 'login') {
  clearAuthentication(null);
} else if (scenario !== 'components') {
  setAuthentication({ accessToken: 'browser-test-memory-token', tokenType: 'Bearer', expiresIn: 60 * 60 });
}

const jsonResponse = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } });

const createPlaceholderPhoto = (photoId: number) => {
  const palettes = [
    ['#c8d4b4', '#73875c'],
    ['#e4d5b8', '#9b805c'],
    ['#c9d6d8', '#6d8588'],
    ['#d8c7c2', '#8b6d64'],
    ['#d5d1bd', '#79735c'],
  ];
  const [start, end] = palettes[photoId % palettes.length];
  const canvas = document.createElement('canvas');
  canvas.width = 480;
  canvas.height = 480;
  const context = canvas.getContext('2d');

  if (context === null) return Promise.resolve(new Blob([], { type: 'image/png' }));

  const gradient = context.createLinearGradient(0, 0, 480, 480);
  gradient.addColorStop(0, start);
  gradient.addColorStop(1, end);
  context.fillStyle = gradient;
  context.fillRect(0, 0, 480, 480);
  context.strokeStyle = 'rgba(255,255,255,.72)';
  context.lineWidth = 18;
  context.lineCap = 'round';
  context.lineJoin = 'round';
  context.beginPath();
  context.moveTo(70, 345);
  context.lineTo(180, 225);
  context.lineTo(252, 295);
  context.lineTo(300, 250);
  context.lineTo(410, 345);
  context.stroke();
  context.fillStyle = 'rgba(255,255,255,.72)';
  context.beginPath();
  context.arc(165, 150, 38, 0, Math.PI * 2);
  context.fill();
  context.fillStyle = 'rgba(255,255,255,.9)';
  context.font = '28px sans-serif';
  context.textAlign = 'center';
  context.fillText(`PHOTO ${photoId - 80}`, 240, 420);

  return new Promise<Blob>((resolve) => {
    canvas.toBlob((blob) => resolve(blob ?? new Blob([], { type: 'image/png' })), 'image/png');
  });
};

const photos = Array.from({ length: 8 }, (_, index) => ({
  ...photoFixture,
  photoId: 81 + index,
  contentUrl: `/api/properties/10/photos/${81 + index}/content`,
  createdAt: `2026-08-10T07:${String(35 + index).padStart(2, '0')}:00Z`,
}));

let conflictReturned = false;
let memoConflictReturned = false;
let statusSaveAttempts = 0;
let memoSaveAttempts = 0;
let checklistSaveAttempts = 0;
const browserRequests: string[] = [];
Object.assign(window, { __browserTestRequests: browserRequests });
let createdChecklist = {
  ...mixedChecklistDetailFixture,
  checklistId: 9,
  name: '브라우저 생성 체크리스트',
} as ChecklistDetail;

const conflictVisitFixture = {
  ...visitDetailFixture,
  stages: visitDetailFixture.stages.map((stage) =>
    stage.stage !== 'ONLINE_PHONE'
      ? stage
      : {
          ...stage,
          items: stage.items.map((item) =>
            item.visitItemId === 501
              ? {
                  ...item,
                  status: 'CAUTION',
                  statusVersion: 1,
                  statusSavedAt: '2026-08-11T04:02:00Z',
                  version: 1,
                  savedAt: '2026-08-11T04:02:00Z',
                }
              : item,
          ),
          summary: { totalCount: 1, checkedCount: 1, goodCount: 0, cautionCount: 1, unconfirmedCount: 0 },
        },
  ),
  summary: { totalCount: 3, checkedCount: 2, goodCount: 1, cautionCount: 1, unconfirmedCount: 1 },
};

const completedVisitFixture = {
  ...visitDetailFixture,
  status: 'COMPLETED',
  completedAt: '2026-08-11T04:05:00Z',
  updatedAt: '2026-08-11T04:05:00Z',
};

const browserVisitFixture = {
  ...visitDetailFixture,
  stages: visitDetailFixture.stages.map((stage) => ({
    ...stage,
    items: stage.items.map((item) =>
      item.visitItemId === 501
        ? {
            ...item,
            question: '관리비 포함 항목과 계절별 추가 비용, 납부 방식, 장기 부재 시 정산 기준까지 모두 확인했나요?',
          }
        : item.visitItemId === 502
          ? {
              ...item,
              inlineMemo: '🏠'.repeat(200),
              memoVersion: 2,
              memoSavedAt: '2026-08-11T04:02:30Z',
            }
          : { ...item, inlineMemo: '  공백 포함 메모  ', memoVersion: 1, memoSavedAt: '2026-08-11T04:02:30Z' },
    ),
  })),
};

const memoConflictVisitFixture = {
  ...browserVisitFixture,
  stages: browserVisitFixture.stages.map((stage) => ({
    ...stage,
    items: stage.items.map((item) =>
      item.visitItemId === 501
        ? {
            ...item,
            inlineMemo: '다른 기기에서 먼저 저장한 메모',
            memoVersion: 1,
            memoSavedAt: '2026-08-11T04:03:00Z',
          }
        : item,
    ),
  })),
};

const browserTestFetch: typeof fetch = async (input, init) => {
  const request = new Request(input, init);
  const url = new URL(request.url);
  const path = url.pathname;

  if (path === '/api/members/me' && request.method === 'GET') {
    if (scenario === 'unauthorized') return jsonResponse(errorEnvelope('ACCESS_TOKEN_EXPIRED'), 401);
    return jsonResponse(successEnvelope(memberFixture));
  }

  if (path === '/api/properties' && request.method === 'GET') {
    const content = scenario === 'list-empty' ? [] : [propertySummaryFixture, secondPropertySummaryFixture];
    return jsonResponse(successEnvelope(propertyPageFixture(content)));
  }

  if (path === '/api/properties' && request.method === 'POST') {
    return jsonResponse(
      successEnvelope({
        propertyId: 10,
        name: '브라우저 등록 매물',
        depositAmount: 10_000_000,
        monthlyRentAmount: 550_000,
        discoverySource: { type: 'TEXT', value: '동네 중개사 추천' },
        createdAt: '2026-08-11T04:00:00Z',
      }),
      201,
    );
  }

  if (path === '/api/properties/10' && request.method === 'GET') {
    const memo =
      scenario === 'memo-empty'
        ? {
            viewingSchedule: '',
            moveInAvailability: '',
            provisionalDeposit: '',
            roomOptions: '',
            maintenanceAndUtilities: '',
            commuteTime: '',
            governmentSupport: '',
            additionalMemo: '',
            content: '',
            savedAt: null,
          }
        : propertyDetailFixture.memo;
    return jsonResponse(
      successEnvelope({
        ...propertyDetailFixture,
        memo,
        name: '긴 이름도 줄바꿈되는 신림역 근처 채광 좋은 원룸 매물',
        depositAmount: Number.MAX_SAFE_INTEGER,
        discoverySource: {
          type: 'TEXT',
          value:
            '동네를 걷다가 발견한 중개사에서 소개받은 매우 긴 발견 경로 설명입니다. 화면 밖으로 넘치지 않아야 합니다.',
        },
        photoPreview:
          scenario === 'photos-empty' ? { totalCount: 0, photos: [] } : { totalCount: 5, photos: photos.slice(0, 5) },
      }),
    );
  }

  if (path === '/api/properties/10/memo' && request.method === 'PUT') {
    if (scenario === 'memo-failure') return jsonResponse(errorEnvelope('INTERNAL_SERVER_ERROR'), 500);
    const body = (await request.json()) as Record<string, unknown>;
    const readMemoField = (key: string) => (typeof body[key] === 'string' ? body[key] : '');
    const additionalMemo = readMemoField('additionalMemo');
    return jsonResponse(
      successEnvelope({
        viewingSchedule: readMemoField('viewingSchedule'),
        moveInAvailability: readMemoField('moveInAvailability'),
        provisionalDeposit: readMemoField('provisionalDeposit'),
        roomOptions: readMemoField('roomOptions'),
        maintenanceAndUtilities: readMemoField('maintenanceAndUtilities'),
        commuteTime: readMemoField('commuteTime'),
        governmentSupport: readMemoField('governmentSupport'),
        additionalMemo,
        content: additionalMemo,
        savedAt: '2026-08-11T04:20:00Z',
      }),
    );
  }

  if (path === '/api/properties/10/visits' && request.method === 'GET') {
    return jsonResponse(
      successEnvelope(
        visitPageFixture([
          {
            visitId: 31,
            status: scenario === 'visit-completed' ? 'COMPLETED' : 'IN_PROGRESS',
            startedAt: visitDetailFixture.startedAt,
            completedAt: scenario === 'visit-completed' ? '2026-08-11T04:05:00Z' : null,
            summary: visitDetailFixture.summary,
          },
          { ...propertyDetailFixture.recentVisit, visitId: 29 },
        ]),
      ),
    );
  }

  if (path === '/api/properties/10/visits' && request.method === 'POST') {
    return jsonResponse(successEnvelope(visitDetailFixture), 201);
  }

  if (path === '/api/visits/31' && request.method === 'GET') {
    if (scenario === 'visit-conflict' && conflictReturned) return jsonResponse(successEnvelope(conflictVisitFixture));
    if (scenario === 'visit-memo-conflict' && memoConflictReturned) {
      return jsonResponse(successEnvelope(memoConflictVisitFixture));
    }
    return jsonResponse(successEnvelope(scenario === 'visit-completed' ? completedVisitFixture : browserVisitFixture));
  }

  if (/^\/api\/visits\/31\/items\/\d+$/.test(path) && request.method === 'PATCH') {
    statusSaveAttempts += 1;
    browserRequests.push(`status:${statusSaveAttempts}`);
    if (scenario === 'visit-save-failure' && statusSaveAttempts === 1) {
      return jsonResponse(errorEnvelope('INTERNAL_SERVER_ERROR'), 500);
    }
    if (scenario === 'visit-conflict' && !conflictReturned) {
      conflictReturned = true;
      return jsonResponse(errorEnvelope('VISIT_ITEM_STATUS_VERSION_CONFLICT'), 409);
    }

    const visitItemId = Number(path.split('/').at(-1));
    const body: unknown = await request.json();
    const status =
      typeof body === 'object' && body !== null && 'status' in body && typeof body.status === 'string'
        ? body.status
        : 'UNCONFIRMED';
    const version =
      typeof body === 'object' &&
      body !== null &&
      'expectedStatusVersion' in body &&
      typeof body.expectedStatusVersion === 'number'
        ? body.expectedStatusVersion + 1
        : 1;
    return jsonResponse(
      successEnvelope({
        item: {
          visitItemId,
          status,
          statusVersion: version,
          statusSavedAt: '2026-08-11T04:08:00Z',
          version,
          savedAt: '2026-08-11T04:08:00Z',
        },
        stageSummary: {
          totalCount: visitItemId === 501 ? 1 : 2,
          checkedCount: 1,
          goodCount: 1,
          cautionCount: 0,
          unconfirmedCount: visitItemId === 501 ? 0 : 1,
        },
        visitSummary: { totalCount: 3, checkedCount: 2, goodCount: 2, cautionCount: 0, unconfirmedCount: 1 },
      }),
    );
  }

  if (/^\/api\/visits\/31\/items\/\d+\/memo$/.test(path) && request.method === 'PATCH') {
    memoSaveAttempts += 1;
    browserRequests.push(`memo:${memoSaveAttempts}`);
    if (scenario === 'visit-completion-failure') {
      return jsonResponse(errorEnvelope('INTERNAL_SERVER_ERROR'), 500);
    }
    if (scenario === 'visit-save-failure' && memoSaveAttempts === 1) {
      return jsonResponse(errorEnvelope('INTERNAL_SERVER_ERROR'), 500);
    }
    if (scenario === 'visit-memo-conflict' && !memoConflictReturned) {
      memoConflictReturned = true;
      return jsonResponse(errorEnvelope('VISIT_ITEM_MEMO_VERSION_CONFLICT'), 409);
    }
    const visitItemId = Number(path.split('/').at(-2));
    const body = (await request.json()) as { memo?: unknown; expectedMemoVersion?: unknown };
    const memo = typeof body.memo === 'string' ? body.memo : '';
    const version = typeof body.expectedMemoVersion === 'number' ? body.expectedMemoVersion + 1 : 1;
    return jsonResponse(
      successEnvelope({
        visitItemId,
        memo,
        memoVersion: version,
        memoSavedAt: '2026-08-11T04:08:30Z',
      }),
    );
  }

  if (path === '/api/visits/31' && request.method === 'PATCH') {
    browserRequests.push('completion');
    return jsonResponse(
      successEnvelope({
        visitId: 31,
        status: 'COMPLETED',
        startedAt: visitDetailFixture.startedAt,
        completedAt: '2026-08-11T04:05:00Z',
        summary: visitDetailFixture.summary,
      }),
    );
  }

  if (path === '/api/check-items' && request.method === 'GET') {
    return jsonResponse(successEnvelope(checkItemPageFixture(presetFixture.items)));
  }

  if (path === '/api/checklist-presets' && request.method === 'GET') {
    return jsonResponse(successEnvelope(presetFixture));
  }

  if (path === '/api/checklists' && request.method === 'GET') {
    return jsonResponse(
      successEnvelope(checklistPageFixture([checklistSummaryFixture, secondChecklistSummaryFixture])),
    );
  }

  if (path === '/api/checklists' && request.method === 'POST') {
    const body = (await request.json()) as {
      name?: unknown;
      stage?: unknown;
      items?: { origin?: unknown; sourceCheckItemId?: unknown; question?: unknown }[];
    };
    const items = (body.items ?? []).map((item, index) => {
      if (item.origin === 'PROVIDED') {
        const sourceCheckItemId = typeof item.sourceCheckItemId === 'number' ? item.sourceCheckItemId : 101;
        return {
          ...providedChecklistItemFixture,
          origin: 'PROVIDED' as const,
          checklistItemId: 901 + index,
          sourceCheckItemId,
          checkItemId: sourceCheckItemId,
          question:
            sourceCheckItemId === onlineItemFixture.checkItemId
              ? onlineItemFixture.question
              : presetFixture.items[1]?.question,
          guide:
            sourceCheckItemId === onlineItemFixture.checkItemId
              ? onlineItemFixture.guide
              : presetFixture.items[1]?.guide,
          order: index + 1,
        };
      }
      return {
        ...customChecklistItemFixture,
        origin: 'CUSTOM' as const,
        checklistItemId: 901 + index,
        question: typeof item.question === 'string' ? item.question : '',
        order: index + 1,
      };
    });
    createdChecklist = {
      ...mixedChecklistDetailFixture,
      checklistId: 9,
      name: typeof body.name === 'string' ? body.name : '브라우저 생성 체크리스트',
      stage: body.stage === 'ONLINE_PHONE' ? body.stage : 'ONLINE_PHONE',
      items,
      itemCount: items.length,
    };
    return jsonResponse(successEnvelope(createdChecklist), 201);
  }

  if (path === '/api/checklists/9' && request.method === 'GET') {
    return jsonResponse(successEnvelope(createdChecklist));
  }

  if (path === '/api/checklists/7' && request.method === 'GET') {
    const inactiveItem = {
      ...providedChecklistItemFixture,
      checklistItemId: 799,
      sourceCheckItemId: 999,
      checkItemId: 999,
      question: '기존에만 남아 있는 매우 긴 비활성 제공 항목으로 유지와 제거 여부를 직접 결정해야 합니다.',
      order: 1,
    };
    const manyCustomItems = Array.from({ length: 16 }, (_, index) => ({
      ...customChecklistItemFixture,
      checklistItemId: 800 + index,
      question: index === 0 ? `🏠${'가'.repeat(199)}` : `직접 추가한 확인 질문 ${index + 1}`,
      order: index + 2,
    }));
    const items =
      scenario === 'checklist-detail-inactive'
        ? [inactiveItem, customChecklistItemFixture]
        : scenario === 'checklist-detail-many'
          ? [providedChecklistItemFixture, ...manyCustomItems]
          : mixedChecklistDetailFixture.items;
    return jsonResponse(
      successEnvelope({
        ...mixedChecklistDetailFixture,
        name: '긴 이름도 안전하게 표시되는 온라인과 전화로 미리 확인할 체크리스트',
        items,
        itemCount: items.length,
      }),
    );
  }

  if (path === '/api/checklists/7' && request.method === 'PUT') {
    checklistSaveAttempts += 1;
    if (scenario === 'checklist-save-failure' && checklistSaveAttempts === 1) {
      return jsonResponse(errorEnvelope('INTERNAL_SERVER_ERROR'), 500);
    }
    const body = (await request.json()) as {
      name?: unknown;
      items?: { origin?: unknown; checklistItemId?: unknown; sourceCheckItemId?: unknown; question?: unknown }[];
    };
    const items = (body.items ?? []).map((item, index) =>
      item.origin === 'PROVIDED'
        ? {
            ...providedChecklistItemFixture,
            checklistItemId: item.sourceCheckItemId === 101 ? 701 : 900 + index,
            sourceCheckItemId: item.sourceCheckItemId,
            checkItemId: item.sourceCheckItemId,
            order: index + 1,
          }
        : {
            ...customChecklistItemFixture,
            checklistItemId: typeof item.checklistItemId === 'number' ? item.checklistItemId : 900 + index,
            question: typeof item.question === 'string' ? item.question : '',
            order: index + 1,
          },
    );
    return jsonResponse(
      successEnvelope({
        ...mixedChecklistDetailFixture,
        name: typeof body.name === 'string' ? body.name : mixedChecklistDetailFixture.name,
        items,
        itemCount: items.length,
        updatedAt: '2026-08-12T12:00:00Z',
      }),
    );
  }

  if (path === '/api/checklists/7' && request.method === 'DELETE') {
    return new Response(null, { status: 204 });
  }

  if (path === '/api/properties/10/active-checklists/ONLINE_PHONE' && request.method === 'PUT') {
    return jsonResponse(
      successEnvelope({
        propertyId: 10,
        stage: 'ONLINE_PHONE',
        checklistId: 7,
        name: checklistSummaryFixture.name,
        itemCount: checklistSummaryFixture.itemCount,
      }),
    );
  }

  if (path === '/api/properties/10/active-checklists/ONLINE_PHONE' && request.method === 'DELETE') {
    return new Response(null, { status: 204 });
  }

  if (path === '/api/properties/10' && request.method === 'DELETE') {
    return new Response(null, { status: 204 });
  }

  if (path === '/api/properties/10/photos' && request.method === 'GET') {
    const selectedPhotos = scenario === 'photos-empty' ? [] : photos;
    return jsonResponse(successEnvelope({ photos: selectedPhotos, totalCount: selectedPhotos.length }));
  }

  if (path === '/api/properties/10/photos' && request.method === 'POST') {
    return jsonResponse(successEnvelope(photos[0]), 201);
  }

  if (/^\/api\/properties\/10\/photos\/\d+\/content$/.test(path) && request.method === 'GET') {
    if (scenario === 'photo-read-failure') return jsonResponse(errorEnvelope('PHOTO_READ_FAILED'), 500);
    const photoId = Number(path.match(/photos\/(\d+)\/content$/)?.[1] ?? 81);
    return new Response(await createPlaceholderPhoto(photoId), { headers: { 'Content-Type': 'image/png' } });
  }

  if (/^\/api\/properties\/10\/photos\/\d+$/.test(path) && request.method === 'DELETE') {
    return new Response(null, { status: 204 });
  }

  return jsonResponse(errorEnvelope('INTERNAL_SERVER_ERROR'), 500);
};

window.fetch = browserTestFetch;

const rootElement = document.getElementById('root');

if (rootElement === null) {
  throw new Error('브라우저 테스트 루트 요소를 찾을 수 없습니다.');
}

createRoot(rootElement).render(
  <StrictMode>
    {scenario === 'components' ? (
      <MemoryRouter>
        <ComponentCatalog />
      </MemoryRouter>
    ) : (
      <App
        config={{
          apiBaseUrl: 'http://localhost:8080',
          googleClientId: 'browser-test-client',
          googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
        }}
      />
    )}
  </StrictMode>,
);
