import { HttpResponse } from 'msw';
import { isChecklistStage } from '../constants/checklist';
import type { ChecklistStage } from '../types/Checklist';

export const now = '2026-08-20T05:00:00.000Z';

export const success = (data: unknown, status = 200) =>
  HttpResponse.json({ code: 'SUCCESS', message: '요청에 성공했습니다.', data }, { status });

export const failure = (code: string, status: number) =>
  HttpResponse.json({ code, message: '요청을 처리하지 못했습니다.', errors: [] }, { status });

export const notImplemented = () => failure('NOT_IMPLEMENTED', 501);

export const mockPhotoBase64 = [
  'iVBORw0KGgoAAAANSUhEUgAAAKAAAAB4CAYAAAB1ovlvAAAB4ElEQVR4nO3SsU0EUAwFQRqnAtqgJiIKICBEECAEFRAgdLff5wk2t/zm7vnl6VuquqsP0O4AVBqASgNQaQAqDUClAag0AJUGoNIAVBqASgNQaQAqDUClAag0AJX2L4Bfn+83Vz3ItgAEEMCTqgfZFoAAAnhS9SDbAhBAAE+qHmRbAAII4EnVg2wLQAABPKl6kG0BCCCAJ1UPsi0AAQTwpOpBtgUggACeVD3ItgAEEMCTqgfZFoAAAnhS9SDbAhBAAE+qHmRbAAII4EnVg2wLQAABPKlLPPnh8X5NAAII4OQABBDAwQEIIICTAxBAAAcHIIAATg5AAAEcHIAAAjg5AAEEcHAAAgjg5AAEEMDBAQgggJMDEEAABwcggABODkAAARzcVQEKQAAHVKMAcHk1CgCXV6MAcHk1CgCXV6MAcHk1CgCXV6MAcHk1CgCXV6MAcHk1CgCXV6MAcHk1CgCXV6M4GuDH2+u3LluN4pr99TcAAgjgrVejAHB5NQoAl1ejAFD6JQCVBqDSAFQagEoDUGkAKg1ApQGoNACVBqDSAFQagEoDUGkAKg1ApQGoNACVBqDSAFQagEoDUGkAKg1ApQGoNACVBqDSAFQagEoDUGkAKg1ApQGoNACVBqDSAFTaD3HcMX7tBIM+AAAAAElFTkSuQmCC',
  'iVBORw0KGgoAAAANSUhEUgAAAKAAAAB4CAYAAAB1ovlvAAAB3klEQVR4nO3SIU4DUBBFUbZKECwXR4LF1zVBlhUgCGnvH+aI6yfzztPH+9tNqnqqD9DuAFQagEoDUGkAKg1ApQGoNACVBqDSAFQagEoDUGkAKg1ApQGoNACV9ieAX9fLv6seZFsAAgjgSdWDbAtAAAE8qXqQbQEIIIAnVQ+yLQABBPCk6kG2BSCAAJ5UPci2AAQQwJOqB9kWgAACeFL1INsCEEAAT6oeZFsAAgjgSdWDbAtAAAE8qXqQbQEIIIAnVQ+yLQABBPCk6kG2BSCAAJ7UPZ788vq8JgABBHByAAII4OAABBDAyQEIIICDAxBAACcHIIAADg5AAAGcHIAAAjg4AAEEcHIAAgjg4AAEEMDJAQgggIMDEEAAJwcggAAO7qEABSCAA6pRALi8GgWAy6tRALi8GgWAy6tRALi8GgWAy6tRALi8GgWAy6tRALi8GgWAy6tRALi8GgWAy6tRHA3wevm86b7VKB7Zb38DIIAA/vdqFAAur0YB4PJqFABKPwSg0gBUGoBKA1BpACoNQKUBqDQAlQag0gBUGoBKA1BpACoNQKUBqDQAlQag0gBUGoBKA1BpACoNQKUBqDQAlQag0gBUGoBKA1BpACoNQKUBqDQAlQag0gBUGoBK+wbRpUHiU7hgNQAAAABJRU5ErkJggg==',
] as const;

export const createMockPhotoBytes = (photoId: number) => {
  const encoded = mockPhotoBase64[photoId % mockPhotoBase64.length] ?? mockPhotoBase64[0];
  return Uint8Array.from(atob(encoded), (character) => character.charCodeAt(0));
};

export type MockCheckItem = {
  id: number;
  stage: ChecklistStage;
  itemType: 'CORE' | 'OPTIONAL';
  question: string;
};

export const checkItems: MockCheckItem[] = [
  { id: 101, stage: 'ONLINE_PHONE', itemType: 'CORE', question: '관리비에 포함된 항목은 무엇인가요?' },
  { id: 102, stage: 'ONLINE_PHONE', itemType: 'CORE', question: '입주 가능한 날짜는 언제인가요?' },
  { id: 103, stage: 'ONLINE_PHONE', itemType: 'OPTIONAL', question: '주차가 가능한가요?' },
  { id: 201, stage: 'ON_SITE', itemType: 'CORE', question: '수압이 충분한가요?' },
  { id: 202, stage: 'ON_SITE', itemType: 'CORE', question: '방음 상태는 괜찮은가요?' },
  { id: 203, stage: 'ON_SITE', itemType: 'OPTIONAL', question: '채광이 충분한가요?' },
  { id: 301, stage: 'PRE_CONTRACT', itemType: 'CORE', question: '등기부등본의 소유자가 일치하나요?' },
  { id: 302, stage: 'PRE_CONTRACT', itemType: 'OPTIONAL', question: '특약 사항이 계약서에 반영됐나요?' },
];

export type MockChecklistItem = MockCheckItem & { displayOrder: number; active: boolean };
export type MockChecklist = {
  id: number;
  name: string;
  stage: ChecklistStage;
  items: MockChecklistItem[];
};

export const checklistItemsFor = (stage: ChecklistStage, ids: number[]) =>
  ids.flatMap((id, index) => {
    const item = checkItems.find((candidate) => candidate.id === id && candidate.stage === stage);
    return item === undefined ? [] : [{ ...item, systemCheckItemId: item.id, displayOrder: index + 1, active: true }];
  });

let checklists: MockChecklist[] = [
  {
    id: 7,
    name: '전화 문의 기본 목록',
    stage: 'ONLINE_PHONE',
    items: checklistItemsFor('ONLINE_PHONE', [101, 102, 103]),
  },
  {
    id: 8,
    name: '집에서 확인할 목록',
    stage: 'ON_SITE',
    items: checklistItemsFor('ON_SITE', [201, 202, 203]),
  },
  {
    id: 9,
    name: '계약 전 마지막 확인',
    stage: 'PRE_CONTRACT',
    items: checklistItemsFor('PRE_CONTRACT', [301, 302]),
  },
];

export const checklistDetail = (checklist: MockChecklist) => ({
  id: checklist.id,
  name: checklist.name,
  stage: checklist.stage,
  itemCount: checklist.items.length,
  items: checklist.items.map(({ id, stage: _stage, ...item }) => ({ systemCheckItemId: id, ...item })),
});

export type MockPhoto = {
  id: number;
  propertyId: number;
  url: string;
  contentType: 'image/png';
  sizeBytes: number;
  representative: boolean;
  createdAt: string;
};

export type MockProperty = {
  id: number;
  name: string;
  depositAmount: number;
  monthlyRentAmount: number;
  maintenanceFeeAmount: number | null;
  discoverySource: string | null;
};

let properties: MockProperty[] = [
  {
    id: 10,
    name: '신림역 원룸',
    depositAmount: 10_000_000,
    monthlyRentAmount: 550_000,
    maintenanceFeeAmount: 50_000,
    discoverySource: 'https://example.com/listings/10',
  },
  {
    id: 11,
    name: '망원동 투룸',
    depositAmount: 30_000_000,
    monthlyRentAmount: 750_000,
    maintenanceFeeAmount: null,
    discoverySource: null,
  },
];

export const createMockPhoto = (propertyId: number, photoId: number, representative = false): MockPhoto => ({
  id: photoId,
  propertyId,
  url: `/api/properties/${propertyId}/photos/${photoId}/content`,
  contentType: 'image/png',
  sizeBytes: createMockPhotoBytes(photoId).byteLength,
  representative,
  createdAt: now,
});

let photosByProperty = new Map<number, MockPhoto[]>([
  [10, [createMockPhoto(10, 81, true), createMockPhoto(10, 82)]],
  [11, []],
]);

export const emptyProgress = {
  totalCount: 0,
  completedCount: 0,
  goodCount: 0,
  cautionCount: 0,
  unconfirmedCount: 0,
  progressRate: 0,
};

export type MockAppliedItem = {
  id: number;
  systemCheckItemId: number;
  question: string;
  displayOrder: number;
  status: 'UNCONFIRMED' | 'GOOD' | 'CAUTION';
  memo: string;
};

export type MockAppliedChecklist = {
  id: number;
  propertyId: number;
  sourceChecklistId: number | null;
  checklistName: string;
  stage: ChecklistStage;
  items: MockAppliedItem[];
};

let nextAppliedChecklistId = 53;
let nextAppliedItemId = 804;
let appliedByProperty = new Map<number, Map<ChecklistStage, MockAppliedChecklist>>([
  [
    10,
    new Map([
      [
        'ONLINE_PHONE',
        {
          id: 51,
          propertyId: 10,
          sourceChecklistId: 7,
          checklistName: '전화 문의 기본 목록',
          stage: 'ONLINE_PHONE',
          items: [
            {
              id: 801,
              systemCheckItemId: 101,
              question: '관리비에 포함된 항목은 무엇인가요?',
              displayOrder: 1,
              status: 'GOOD',
              memo: '수도 요금 포함',
            },
            {
              id: 802,
              systemCheckItemId: 102,
              question: '입주 가능한 날짜는 언제인가요?',
              displayOrder: 2,
              status: 'CAUTION',
              memo: '날짜 재확인 필요',
            },
            {
              id: 803,
              systemCheckItemId: 103,
              question: '주차가 가능한가요?',
              displayOrder: 3,
              status: 'UNCONFIRMED',
              memo: '',
            },
          ],
        },
      ],
    ]),
  ],
]);

export const progressFromItems = (items: MockAppliedItem[]) => {
  const completedCount = items.filter((item) => item.status !== 'UNCONFIRMED').length;
  return {
    totalCount: items.length,
    completedCount,
    goodCount: items.filter((item) => item.status === 'GOOD').length,
    cautionCount: items.filter((item) => item.status === 'CAUTION').length,
    unconfirmedCount: items.filter((item) => item.status === 'UNCONFIRMED').length,
    progressRate: items.length === 0 ? 0 : Math.floor((completedCount / items.length) * 100),
  };
};

export const propertyProgress = (propertyId: number) => {
  const items = Array.from(appliedByProperty.get(propertyId)?.values() ?? []).flatMap((checklist) => checklist.items);
  return progressFromItems(items);
};

export const propertyResponse = (property: MockProperty) => {
  const photos = photosByProperty.get(property.id) ?? [];
  const basicInfo = { ...property, maintenanceFeeAmount: undefined };
  return {
    ...basicInfo,
    photos,
    representativePhoto: photos.find((photo) => photo.representative) ?? null,
    overallProgress: propertyProgress(property.id),
  };
};

export const systemMemoItems = [
  { id: 1, label: '집 주소', displayOrder: 1 },
  { id: 2, label: '입주 가능일', displayOrder: 2 },
  { id: 3, label: '가계약금', displayOrder: 3 },
  { id: 4, label: '방 옵션', displayOrder: 4 },
  { id: 5, label: '관리비 및 공과금', displayOrder: 5 },
  { id: 6, label: '통학 통근 시간', displayOrder: 6 },
];

export type MockMemo = {
  propertyId: number;
  items: Array<{
    propertyMemoItemId: number;
    systemMemoItemId: number;
    label: string;
    displayOrder: number;
    content: string;
  }>;
  freeMemo: string;
};

export const emptyMemo = (propertyId: number): MockMemo => ({
  propertyId,
  items: systemMemoItems.map(({ id, ...item }, index) => ({
    propertyMemoItemId: propertyId * 100 + index + 1,
    systemMemoItemId: id,
    ...item,
    content: '',
  })),
  freeMemo: '',
});

let memoByProperty = new Map<number, MockMemo>([
  [
    10,
    {
      ...emptyMemo(10),
      items: emptyMemo(10).items.map((item) =>
        item.systemMemoItemId === 1 ? { ...item, content: '관악구 신림로 12길 3, 302호' } : item,
      ),
      freeMemo: '관리비 포함 항목을 다시 확인하기',
    },
  ],
]);

export const readPositiveInteger = (value: string | readonly string[] | undefined) => {
  if (typeof value !== 'string') return null;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
};

export const readStage = (value: string | readonly string[] | undefined) =>
  typeof value === 'string' && isChecklistStage(value) ? value : null;

export const getProperty = (value: string | readonly string[] | undefined) => {
  const id = readPositiveInteger(value);
  return id === null ? undefined : properties.find((property) => property.id === id);
};

export const obsoleteEndpoint = () => failure('API_CONTRACT_REMOVED', 410);

export const hasDuplicates = (values: number[]) => new Set(values).size !== values.length;

export const getMockChecklists = () => checklists;
export const setMockChecklists = (next: MockChecklist[]) => {
  checklists = next;
};

export const getMockProperties = () => properties;
export const setMockProperties = (next: MockProperty[]) => {
  properties = next;
};

export const getMockPhotosByProperty = () => photosByProperty;
export const setMockPhotosByProperty = (next: Map<number, MockPhoto[]>) => {
  photosByProperty = next;
};

export const getMockMemosByProperty = () => memoByProperty;
export const setMockMemosByProperty = (next: Map<number, MockMemo>) => {
  memoByProperty = next;
};

export const getMockAppliedByProperty = () => appliedByProperty;
export const setMockAppliedByProperty = (next: Map<number, Map<ChecklistStage, MockAppliedChecklist>>) => {
  appliedByProperty = next;
};

export const takeNextAppliedChecklistId = () => nextAppliedChecklistId++;
export const takeNextAppliedItemId = () => nextAppliedItemId++;

const initialChecklists = structuredClone(checklists);
const initialProperties = structuredClone(properties);
const initialPhotosByProperty = structuredClone(photosByProperty);
const initialMemoByProperty = structuredClone(memoByProperty);
const initialAppliedByProperty = structuredClone(appliedByProperty);

export const resetMockStore = () => {
  checklists = structuredClone(initialChecklists);
  properties = structuredClone(initialProperties);
  photosByProperty = structuredClone(initialPhotosByProperty);
  memoByProperty = structuredClone(initialMemoByProperty);
  appliedByProperty = structuredClone(initialAppliedByProperty);
  nextAppliedChecklistId = 53;
  nextAppliedItemId = 804;
};
