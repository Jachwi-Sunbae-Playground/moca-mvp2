import { http, HttpResponse } from 'msw';
import { isChecklistStage } from '../constants/checklist';
import { CHECKLIST_STAGES } from '../types/Checklist';
import type { ChecklistDetail, ChecklistItem, ChecklistStage, CheckItem } from '../types/Checklist';
import type { VisitDetail, VisitItemStatus, VisitSnapshotItem, VisitSummary } from '../types/Visit';
import {
  memberFixture,
  photoFixture,
  propertyDetailFixture,
  propertyPageFixture,
  propertySummaryFixture,
  secondPropertySummaryFixture,
  successEnvelope,
} from '../test/propertyFixtures';
import type { PropertyPhoto } from '../types/Property';
import { visitPageFixture } from '../test/visitFixtures';

const now = '2026-08-17T05:00:00Z';

const mockPhotoBase64 = [
  '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',
] as const;

const createMockPhotoBytes = (photoId: number) => {
  const encoded = mockPhotoBase64[(photoId - 81) % mockPhotoBase64.length] ?? mockPhotoBase64[0];
  return Uint8Array.from(atob(encoded), (character) => character.charCodeAt(0));
};

const checkItemsByStage: Record<ChecklistStage, CheckItem[]> = {
  ONLINE_PHONE: [
    {
      checkItemId: 101,
      stage: 'ONLINE_PHONE',
      question: '관리비에 포함된 항목은 무엇인가요?',
      guide: '수도, 인터넷과 공용 전기 포함 여부를 확인해요.',
    },
    {
      checkItemId: 102,
      stage: 'ONLINE_PHONE',
      question: '입주 가능한 날짜는 언제인가요?',
      guide: '계약 시작일과 실제 입주 가능일을 함께 확인해요.',
    },
    {
      checkItemId: 103,
      stage: 'ONLINE_PHONE',
      question: '주차가 가능한가요?',
      guide: '추가 비용과 지정 주차 여부를 확인해요.',
    },
  ],
  ON_SITE: [
    {
      checkItemId: 201,
      stage: 'ON_SITE',
      question: '수압이 충분한가요?',
      guide: '주방과 욕실 수도를 동시에 틀어 확인해요.',
    },
    {
      checkItemId: 202,
      stage: 'ON_SITE',
      question: '방음 상태는 괜찮은가요?',
      guide: '복도와 창문 쪽 소음을 확인해요.',
    },
  ],
  PRE_CONTRACT: [
    {
      checkItemId: 301,
      stage: 'PRE_CONTRACT',
      question: '등기부등본의 소유자가 일치하나요?',
      guide: '계약 상대방과 등기상 소유자를 비교해요.',
    },
    {
      checkItemId: 302,
      stage: 'PRE_CONTRACT',
      question: '특약 사항이 계약서에 반영됐나요?',
      guide: '구두로 합의한 내용을 계약서에서 다시 확인해요.',
    },
  ],
};

const toProvidedItem = (item: CheckItem, order: number): ChecklistItem => ({
  checklistItemId: item.checkItemId * 10,
  origin: 'PROVIDED',
  sourceCheckItemId: item.checkItemId,
  checkItemId: item.checkItemId,
  question: item.question,
  guide: item.guide,
  order,
});

let checklists: ChecklistDetail[] = [
  {
    checklistId: 7,
    name: '전화 문의 기본 목록',
    stage: 'ONLINE_PHONE',
    items: checkItemsByStage.ONLINE_PHONE.slice(0, 2).map((item, index) => toProvidedItem(item, index + 1)),
    itemCount: 2,
    assignedPropertyCount: 1,
    createdAt: now,
    updatedAt: now,
  },
  {
    checklistId: 8,
    name: '집에서 확인할 목록',
    stage: 'ON_SITE',
    items: checkItemsByStage.ON_SITE.map((item, index) => toProvidedItem(item, index + 1)),
    itemCount: 2,
    assignedPropertyCount: 1,
    createdAt: now,
    updatedAt: now,
  },
  {
    checklistId: 9,
    name: '계약 전 마지막 확인',
    stage: 'PRE_CONTRACT',
    items: checkItemsByStage.PRE_CONTRACT.map((item, index) => toProvidedItem(item, index + 1)),
    itemCount: 2,
    assignedPropertyCount: 0,
    createdAt: now,
    updatedAt: now,
  },
];

const activeChecklistIds = new Map<ChecklistStage, number>([
  ['ONLINE_PHONE', 7],
  ['ON_SITE', 8],
]);

const summarizeVisitItems = (items: VisitSnapshotItem[]): VisitSummary => ({
  totalCount: items.length,
  checkedCount: items.filter(({ status }) => status !== 'UNCONFIRMED').length,
  goodCount: items.filter(({ status }) => status === 'GOOD').length,
  cautionCount: items.filter(({ status }) => status === 'CAUTION').length,
  unconfirmedCount: items.filter(({ status }) => status === 'UNCONFIRMED').length,
});

const refreshVisitSummaries = (detail: VisitDetail): VisitDetail => {
  const stages = detail.stages.map((stage) => ({ ...stage, summary: summarizeVisitItems(stage.items) }));
  return { ...detail, stages, summary: summarizeVisitItems(stages.flatMap(({ items }) => items)) };
};

const createMockVisit = (visitId: number): VisitDetail => {
  let visitItemId = visitId * 100;
  const stages = CHECKLIST_STAGES.flatMap((stage) => {
    const checklistId = activeChecklistIds.get(stage);
    const checklist = checklists.find((candidate) => candidate.checklistId === checklistId);
    if (checklist === undefined) return [];

    const items: VisitSnapshotItem[] = checklist.items.map((item) => ({
      visitItemId: (visitItemId += 1),
      origin: item.origin,
      sourceChecklistItemId: item.checklistItemId,
      sourceCheckItemId: item.sourceCheckItemId,
      question: item.question,
      guide: item.guide,
      order: item.order,
      status: 'UNCONFIRMED',
      statusVersion: 0,
      statusSavedAt: now,
      inlineMemo: '',
      memoVersion: 0,
      memoSavedAt: null,
      version: 0,
      savedAt: now,
    }));

    return [
      {
        stage,
        sourceChecklistId: checklist.checklistId,
        checklistName: checklist.name,
        items,
        summary: summarizeVisitItems(items),
      },
    ];
  });

  return refreshVisitSummaries({
    visitId,
    propertyId: 10,
    status: 'IN_PROGRESS',
    startedAt: now,
    completedAt: null,
    updatedAt: now,
    stages,
    summary: summarizeVisitItems(stages.flatMap(({ items }) => items)),
  });
};

const createInitialMockVisit = (): VisitDetail => {
  const visit = createMockVisit(31);
  const items = visit.stages.flatMap((stage) => stage.items);
  const firstItem = items[0];
  const secondItem = items[1];
  const thirdItem = items[2];

  if (firstItem !== undefined) {
    firstItem.status = 'GOOD';
    firstItem.statusVersion = 1;
    firstItem.version = 1;
    firstItem.inlineMemo = '관리비에 수도 요금이 포함돼요.';
    firstItem.memoVersion = 1;
    firstItem.memoSavedAt = now;
  }
  if (secondItem !== undefined) {
    secondItem.status = 'CAUTION';
    secondItem.statusVersion = 1;
    secondItem.version = 1;
    secondItem.inlineMemo = '입주 가능일을 다시 협의해야 해요.';
    secondItem.memoVersion = 1;
    secondItem.memoSavedAt = now;
  }
  if (thirdItem !== undefined) {
    thirdItem.status = 'GOOD';
    thirdItem.statusVersion = 1;
    thirdItem.version = 1;
  }

  return refreshVisitSummaries({ ...visit, status: 'COMPLETED', completedAt: now, updatedAt: now });
};

let currentVisit = createInitialMockVisit();

const createMockPhoto = (propertyId: number, photoId: number): PropertyPhoto => ({
  ...photoFixture,
  photoId,
  contentUrl: `/api/properties/${propertyId}/photos/${photoId}/content`,
  contentType: 'image/png',
  sizeBytes: createMockPhotoBytes(photoId).byteLength,
  createdAt: now,
});

let mockPhotosByProperty = new Map<number, PropertyPhoto[]>([
  [10, [81, 82, 83, 84].map((photoId) => createMockPhoto(10, photoId))],
  [11, []],
]);

const getMockPhotos = (propertyId: number) => mockPhotosByProperty.get(propertyId) ?? [];

const getPropertyId = (value: string | readonly string[] | undefined): number | null => {
  if (typeof value !== 'string') return null;
  const propertyId = Number(value);
  return Number.isInteger(propertyId) && propertyId > 0 ? propertyId : null;
};

const toRecentVisit = (detail: VisitDetail) => ({
  visitId: detail.visitId,
  status: detail.status,
  startedAt: detail.startedAt,
  completedAt: detail.completedAt,
  summary: detail.summary,
});

const success = (data: unknown, status = 200) => HttpResponse.json(successEnvelope(data), { status });

const page = (content: unknown[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: content.length === 0 ? 0 : 1,
  hasNext: false,
});

const getStage = (url: string): ChecklistStage => {
  const stage = new URL(url).searchParams.get('stage');
  return isChecklistStage(stage) ? stage : 'ONLINE_PHONE';
};

const getChecklistId = (value: string | readonly string[] | undefined): number | null => {
  if (typeof value !== 'string') return null;
  const checklistId = Number(value);
  return Number.isInteger(checklistId) && checklistId > 0 ? checklistId : null;
};

type ChecklistRequestItem = {
  origin?: unknown;
  sourceCheckItemId?: unknown;
  checklistItemId?: unknown;
  question?: unknown;
};

type ChecklistRequest = {
  name?: unknown;
  stage?: unknown;
  items?: unknown;
  checkItemIds?: unknown;
};

const readChecklistItems = (
  body: ChecklistRequest,
  stage: ChecklistStage,
  previousItems: ChecklistItem[] = [],
): ChecklistItem[] => {
  const requestItems: ChecklistRequestItem[] = Array.isArray(body.items)
    ? body.items
    : Array.isArray(body.checkItemIds)
      ? body.checkItemIds.map((sourceCheckItemId) => ({ origin: 'PROVIDED', sourceCheckItemId }))
      : [];

  return requestItems.flatMap((requestItem, index) => {
    if (requestItem.origin === 'CUSTOM' && typeof requestItem.question === 'string') {
      const existingItem = previousItems.find(
        (item) => item.origin === 'CUSTOM' && item.checklistItemId === requestItem.checklistItemId,
      );
      return [
        {
          checklistItemId: existingItem?.checklistItemId ?? 10_000 + index,
          origin: 'CUSTOM',
          sourceCheckItemId: null,
          checkItemId: null,
          question: requestItem.question,
          guide: null,
          order: index + 1,
        },
      ];
    }

    if (requestItem.origin !== 'PROVIDED' || typeof requestItem.sourceCheckItemId !== 'number') return [];
    const source = checkItemsByStage[stage].find((item) => item.checkItemId === requestItem.sourceCheckItemId);
    return source === undefined ? [] : [toProvidedItem(source, index + 1)];
  });
};

const checklistSummary = ({ items: _items, createdAt: _createdAt, ...checklist }: ChecklistDetail) => checklist;

export const handlers = [
  http.get('*/api/members/me', () => success(memberFixture)),

  http.get('*/api/properties', () =>
    success(
      propertyPageFixture([
        {
          ...propertySummaryFixture,
          recentVisit: toRecentVisit(currentVisit),
          photoCount: getMockPhotos(10).length,
        },
        { ...secondPropertySummaryFixture, photoCount: getMockPhotos(11).length },
      ]),
    ),
  ),

  http.get('*/api/properties/:propertyId', ({ params }) => {
    const propertyId = getPropertyId(params.propertyId);
    if (propertyId === null || (propertyId !== 10 && propertyId !== 11)) {
      return new HttpResponse(null, { status: 404 });
    }

    const photos = getMockPhotos(propertyId);
    if (propertyId === 11) {
      return success({
        ...propertyDetailFixture,
        ...secondPropertySummaryFixture,
        memo: {
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
        },
        activeChecklists: [],
        recentVisit: null,
        photoPreview: { totalCount: 0, photos: [] },
        deletionImpact: { visitCount: 0, photoCount: 0, activeChecklistCount: 0 },
      });
    }

    const activeChecklists = CHECKLIST_STAGES.flatMap((stage) => {
      const checklistId = activeChecklistIds.get(stage);
      const checklist = checklists.find((candidate) => candidate.checklistId === checklistId);
      return checklist === undefined
        ? []
        : [{ stage, checklistId: checklist.checklistId, name: checklist.name, itemCount: checklist.itemCount }];
    });
    return success({
      ...propertyDetailFixture,
      activeChecklists,
      recentVisit: toRecentVisit(currentVisit),
      photoPreview: {
        totalCount: photos.length,
        photos: photos.slice(0, 3).map(({ photoId, contentUrl, createdAt }) => ({ photoId, contentUrl, createdAt })),
      },
      deletionImpact: { ...propertyDetailFixture.deletionImpact, photoCount: photos.length },
    });
  }),

  http.get('*/api/properties/:propertyId/photos', ({ params }) => {
    const propertyId = getPropertyId(params.propertyId);
    if (propertyId === null) return new HttpResponse(null, { status: 404 });
    const photos = getMockPhotos(propertyId);
    return success({ photos, totalCount: photos.length });
  }),

  http.post('*/api/properties/:propertyId/photos', ({ params }) => {
    const propertyId = getPropertyId(params.propertyId);
    if (propertyId === null) return new HttpResponse(null, { status: 404 });
    const photos = getMockPhotos(propertyId);
    const photoId = Math.max(80, ...photos.map((photo) => photo.photoId)) + 1;
    const photo = createMockPhoto(propertyId, photoId);
    mockPhotosByProperty = new Map(mockPhotosByProperty).set(propertyId, [...photos, photo]);
    return success(photo, 201);
  }),

  http.delete('*/api/properties/:propertyId/photos/:photoId', ({ params }) => {
    const propertyId = getPropertyId(params.propertyId);
    const photoId = typeof params.photoId === 'string' ? Number(params.photoId) : NaN;
    if (propertyId === null) return new HttpResponse(null, { status: 404 });
    const photos = getMockPhotos(propertyId);
    if (!photos.some((photo) => photo.photoId === photoId)) return new HttpResponse(null, { status: 404 });
    mockPhotosByProperty = new Map(mockPhotosByProperty).set(
      propertyId,
      photos.filter((photo) => photo.photoId !== photoId),
    );
    return new HttpResponse(null, { status: 204 });
  }),

  http.get('*/api/properties/:propertyId/photos/:photoId/content', ({ params }) => {
    const photoId = typeof params.photoId === 'string' ? Number(params.photoId) : 81;
    return new HttpResponse(createMockPhotoBytes(photoId), { headers: { 'Content-Type': 'image/png' } });
  }),

  http.get('*/api/properties/10/visits', () => success(visitPageFixture([toRecentVisit(currentVisit)]))),

  http.post('*/api/properties/10/visits', () => {
    currentVisit = createMockVisit(currentVisit.visitId + 1);
    return success(currentVisit, 201);
  }),

  http.get('*/api/visits/:visitId', ({ params }) => {
    const visitId = typeof params.visitId === 'string' ? Number(params.visitId) : NaN;
    return visitId === currentVisit.visitId ? success(currentVisit) : new HttpResponse(null, { status: 404 });
  }),

  http.patch('*/api/visits/:visitId/items/:visitItemId', async ({ params, request }) => {
    const visitId = typeof params.visitId === 'string' ? Number(params.visitId) : NaN;
    const visitItemId = typeof params.visitItemId === 'string' ? Number(params.visitItemId) : NaN;
    const body = (await request.json()) as { status?: unknown; expectedStatusVersion?: unknown };
    const status: VisitItemStatus | null =
      body.status === 'GOOD' || body.status === 'CAUTION' || body.status === 'UNCONFIRMED' ? body.status : null;
    const stage = currentVisit.stages.find(({ items }) => items.some((item) => item.visitItemId === visitItemId));
    const item = stage?.items.find((candidate) => candidate.visitItemId === visitItemId);
    if (visitId !== currentVisit.visitId || stage === undefined || item === undefined || status === null) {
      return new HttpResponse(null, { status: 404 });
    }

    item.status = status;
    item.statusVersion = typeof body.expectedStatusVersion === 'number' ? body.expectedStatusVersion + 1 : 1;
    item.statusSavedAt = now;
    item.version = item.statusVersion;
    item.savedAt = now;
    currentVisit = refreshVisitSummaries(currentVisit);
    const updatedStage = currentVisit.stages.find((candidate) => candidate.stage === stage.stage);
    return success({
      item: {
        visitItemId,
        status,
        statusVersion: item.statusVersion,
        statusSavedAt: item.statusSavedAt,
        version: item.statusVersion,
        savedAt: item.statusSavedAt,
      },
      stageSummary: updatedStage?.summary ?? summarizeVisitItems([]),
      visitSummary: currentVisit.summary,
    });
  }),

  http.patch('*/api/visits/:visitId/items/:visitItemId/memo', async ({ params, request }) => {
    const visitId = typeof params.visitId === 'string' ? Number(params.visitId) : NaN;
    const visitItemId = typeof params.visitItemId === 'string' ? Number(params.visitItemId) : NaN;
    const body = (await request.json()) as { memo?: unknown; expectedMemoVersion?: unknown };
    const item = currentVisit.stages
      .flatMap(({ items }) => items)
      .find((candidate) => candidate.visitItemId === visitItemId);
    if (visitId !== currentVisit.visitId || item === undefined || typeof body.memo !== 'string') {
      return new HttpResponse(null, { status: 404 });
    }

    item.inlineMemo = body.memo;
    item.memoVersion = typeof body.expectedMemoVersion === 'number' ? body.expectedMemoVersion + 1 : 1;
    item.memoSavedAt = now;
    return success({ visitItemId, memo: item.inlineMemo, memoVersion: item.memoVersion, memoSavedAt: now });
  }),

  http.patch('*/api/visits/:visitId', ({ params }) => {
    const visitId = typeof params.visitId === 'string' ? Number(params.visitId) : NaN;
    if (visitId !== currentVisit.visitId) return new HttpResponse(null, { status: 404 });
    currentVisit = { ...currentVisit, status: 'COMPLETED', completedAt: now, updatedAt: now };
    return success(toRecentVisit(currentVisit));
  }),

  http.get('*/api/check-items', ({ request }) => {
    const stage = getStage(request.url);
    const query = new URL(request.url).searchParams.get('query')?.trim() ?? '';
    const items = checkItemsByStage[stage].filter((item) => item.question.includes(query));
    return success(page(items));
  }),

  http.get('*/api/checklist-presets', ({ request }) => {
    const stage = getStage(request.url);
    const presetType = new URL(request.url).searchParams.get('presetType') === 'GOSHIWON' ? 'GOSHIWON' : 'ONE_ROOM';
    return success({
      presetType,
      stage,
      items: checkItemsByStage[stage].map((item, order) => ({ ...item, order })),
    });
  }),

  http.get('*/api/checklists', ({ request }) => {
    const stage = getStage(request.url);
    return success(page(checklists.filter((checklist) => checklist.stage === stage).map(checklistSummary)));
  }),

  http.post('*/api/checklists', async ({ request }) => {
    const body = (await request.json()) as ChecklistRequest;
    const stage = isChecklistStage(body.stage) ? body.stage : 'ONLINE_PHONE';
    const items = readChecklistItems(body, stage);
    const checklist: ChecklistDetail = {
      checklistId: Math.max(0, ...checklists.map(({ checklistId }) => checklistId)) + 1,
      name: typeof body.name === 'string' ? body.name : '새 체크리스트',
      stage,
      items,
      itemCount: items.length,
      assignedPropertyCount: 0,
      createdAt: now,
      updatedAt: now,
    };
    checklists = [...checklists, checklist];
    return success(checklist, 201);
  }),

  http.get('*/api/checklists/:checklistId', ({ params }) => {
    const checklistId = getChecklistId(params.checklistId);
    const checklist = checklists.find((candidate) => candidate.checklistId === checklistId);
    return checklist === undefined ? new HttpResponse(null, { status: 404 }) : success(checklist);
  }),

  http.put('*/api/checklists/:checklistId', async ({ params, request }) => {
    const checklistId = getChecklistId(params.checklistId);
    const current = checklists.find((candidate) => candidate.checklistId === checklistId);
    if (current === undefined) return new HttpResponse(null, { status: 404 });

    const body = (await request.json()) as ChecklistRequest;
    const items = readChecklistItems(body, current.stage, current.items);
    const updated: ChecklistDetail = {
      ...current,
      name: typeof body.name === 'string' ? body.name : current.name,
      items,
      itemCount: items.length,
      updatedAt: now,
    };
    checklists = checklists.map((checklist) => (checklist.checklistId === checklistId ? updated : checklist));
    return success(updated);
  }),

  http.delete('*/api/checklists/:checklistId', ({ params }) => {
    const checklistId = getChecklistId(params.checklistId);
    checklists = checklists.filter((checklist) => checklist.checklistId !== checklistId);
    return new HttpResponse(null, { status: 204 });
  }),

  http.put('*/api/properties/:propertyId/active-checklists/:stage', async ({ params, request }) => {
    const stage = typeof params.stage === 'string' && isChecklistStage(params.stage) ? params.stage : null;
    const propertyId = typeof params.propertyId === 'string' ? Number(params.propertyId) : NaN;
    const body = (await request.json()) as { checklistId?: unknown };
    const checklist = checklists.find((candidate) => candidate.checklistId === body.checklistId);
    if (stage === null || !Number.isInteger(propertyId) || checklist === undefined) {
      return new HttpResponse(null, { status: 400 });
    }
    activeChecklistIds.set(stage, checklist.checklistId);
    return success({
      propertyId,
      stage,
      checklistId: checklist.checklistId,
      name: checklist.name,
      itemCount: checklist.itemCount,
    });
  }),

  http.delete('*/api/properties/:propertyId/active-checklists/:stage', ({ params }) => {
    if (typeof params.stage === 'string' && isChecklistStage(params.stage)) activeChecklistIds.delete(params.stage);
    return new HttpResponse(null, { status: 204 });
  }),
];
