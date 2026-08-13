export const onlineItemFixture = {
  checkItemId: 101,
  stage: 'ONLINE_PHONE',
  question: '관리비에 포함된 항목은 무엇인가요?',
  guide: '수도, 인터넷과 공용 전기 포함 여부를 확인해요.',
};

export const secondOnlineItemFixture = {
  checkItemId: 102,
  stage: 'ONLINE_PHONE',
  question: '입주 가능한 날짜는 언제인가요?',
  guide: '계약 시작일과 실제 입주 가능일을 함께 확인해요.',
};

export const checkItemPageFixture = (content: unknown[], page = 0, hasNext = false) => ({
  content,
  page,
  size: 20,
  totalElements: content.length + (hasNext ? 1 : 0),
  totalPages: hasNext ? page + 2 : Math.max(1, page + 1),
  hasNext,
});

export const presetFixture = {
  presetType: 'ONE_ROOM',
  stage: 'ONLINE_PHONE',
  items: [
    { ...onlineItemFixture, order: 0 },
    { ...secondOnlineItemFixture, order: 1 },
  ],
};

export const checklistSummaryFixture = {
  checklistId: 7,
  name: '전화 문의 기본 목록',
  stage: 'ONLINE_PHONE',
  itemCount: 2,
  assignedPropertyCount: 1,
  updatedAt: '2026-08-11T05:00:00Z',
};

export const secondChecklistSummaryFixture = {
  ...checklistSummaryFixture,
  checklistId: 8,
  name: '직방 매물 문의 목록',
  assignedPropertyCount: 0,
};

export const checklistPageFixture = (content: unknown[], page = 0, hasNext = false) => ({
  content,
  page,
  size: 20,
  totalElements: content.length + (hasNext ? 1 : 0),
  totalPages: hasNext ? page + 2 : Math.max(1, page + 1),
  hasNext,
});

export const providedChecklistItemFixture = {
  checklistItemId: 701,
  origin: 'PROVIDED',
  sourceCheckItemId: 101,
  checkItemId: 101,
  question: onlineItemFixture.question,
  guide: onlineItemFixture.guide,
  order: 1,
};

export const secondProvidedChecklistItemFixture = {
  checklistItemId: 702,
  origin: 'PROVIDED',
  sourceCheckItemId: 102,
  checkItemId: 102,
  question: secondOnlineItemFixture.question,
  guide: secondOnlineItemFixture.guide,
  order: 2,
};

export const customChecklistItemFixture = {
  checklistItemId: 703,
  origin: 'CUSTOM',
  sourceCheckItemId: null,
  checkItemId: null,
  question: '창틀 곰팡이는 괜찮은가?',
  guide: null,
  order: 2,
};

export const checklistDetailFixture = {
  ...checklistSummaryFixture,
  items: [providedChecklistItemFixture, secondProvidedChecklistItemFixture],
  createdAt: '2026-08-11T04:30:00Z',
};

export const mixedChecklistDetailFixture = {
  ...checklistDetailFixture,
  items: [providedChecklistItemFixture, customChecklistItemFixture],
};
