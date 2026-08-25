export const onlineItemFixture = {
  id: 101,
  checkItemId: 101,
  stage: 'ONLINE_PHONE',
  itemType: 'CORE' as const,
  question: '관리비에 포함된 항목은 무엇인가요?',
  guide: '수도, 인터넷과 공용 전기 포함 여부를 확인해요.',
};

export const secondOnlineItemFixture = {
  id: 102,
  checkItemId: 102,
  stage: 'ONLINE_PHONE',
  itemType: 'OPTIONAL' as const,
  question: '입주 가능한 날짜는 언제인가요?',
  guide: '계약 시작일과 실제 입주 가능일을 함께 확인해요.',
};

export const checkItemPageFixture = (content: unknown[]) => content;

export const presetFixture = {
  presetType: 'ONE_ROOM',
  stage: 'ONLINE_PHONE',
  items: [
    { ...onlineItemFixture, order: 0 },
    { ...secondOnlineItemFixture, order: 1 },
  ],
};

export const checklistSummaryFixture = {
  id: 7,
  checklistId: 7,
  name: '전화 문의 기본 목록',
  stage: 'ONLINE_PHONE',
  itemCount: 2,
  assignedPropertyCount: 1,
  updatedAt: '2026-08-11T05:00:00Z',
};

export const secondChecklistSummaryFixture = {
  ...checklistSummaryFixture,
  id: 8,
  checklistId: 8,
  name: '직방 매물 문의 목록',
  assignedPropertyCount: 0,
};

export const checklistPageFixture = (content: unknown[]) => ({
  totalCount: content.length,
  items: content,
});

export const providedChecklistItemFixture = {
  id: 701,
  systemCheckItemId: 101,
  displayOrder: 1,
  checklistItemId: 701,
  origin: 'PROVIDED',
  sourceCheckItemId: 101,
  checkItemId: 101,
  itemType: 'CORE' as const,
  question: onlineItemFixture.question,
  guide: onlineItemFixture.guide,
  order: 1,
  active: true,
};

export const secondProvidedChecklistItemFixture = {
  id: 702,
  systemCheckItemId: 102,
  displayOrder: 2,
  checklistItemId: 702,
  origin: 'PROVIDED',
  sourceCheckItemId: 102,
  checkItemId: 102,
  itemType: 'OPTIONAL' as const,
  question: secondOnlineItemFixture.question,
  guide: secondOnlineItemFixture.guide,
  order: 2,
  active: true,
};

export const customChecklistItemFixture = {
  id: 703,
  systemCheckItemId: null,
  displayOrder: 2,
  checklistItemId: 703,
  origin: 'CUSTOM',
  sourceCheckItemId: null,
  checkItemId: null,
  itemType: 'OPTIONAL' as const,
  question: '창틀 곰팡이는 괜찮은가?',
  guide: null,
  order: 2,
  active: true,
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
