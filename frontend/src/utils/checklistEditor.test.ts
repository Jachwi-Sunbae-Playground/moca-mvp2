import { describe, expect, it } from 'vitest';
import { checkItemToEditorItem, checklistItemToEditorItem } from '../types/ChecklistEditor';
import {
  editorItemsFingerprint,
  moveEditorItem,
  toCreateChecklistItems,
  toUpdateChecklistItems,
} from './checklistEditor';
import { onlineItemFixture, providedChecklistItemFixture, secondOnlineItemFixture } from '../test/checklistFixtures';
import type { CheckItem, ChecklistItem } from '../types/Checklist';

describe('체크리스트 편집 상태와 DTO 변환', () => {
  const optionalItem = checkItemToEditorItem(onlineItemFixture as CheckItem);
  const anotherOptionalItem = checkItemToEditorItem(secondOnlineItemFixture as CheckItem);
  const existingItem = checklistItemToEditorItem(providedChecklistItemFixture as ChecklistItem);

  it('생성 요청에는 선택한 OPTIONAL 시스템 항목 ID만 순서대로 담는다', () => {
    expect(toCreateChecklistItems([optionalItem, anotherOptionalItem])).toEqual([
      anotherOptionalItem.sourceCheckItemId,
    ]);
  });

  it('수정 요청에는 현재 시스템 항목 ID를 최종 표시 순서대로 담는다', () => {
    expect(toUpdateChecklistItems([anotherOptionalItem, existingItem, optionalItem])).toEqual([
      anotherOptionalItem.sourceCheckItemId,
      existingItem.sourceCheckItemId,
      optionalItem.sourceCheckItemId,
    ]);
  });

  it('항목 식별자와 순서가 달라지면 편집 지문도 달라진다', () => {
    expect(editorItemsFingerprint([optionalItem, anotherOptionalItem])).not.toBe(
      editorItemsFingerprint([anotherOptionalItem, optionalItem]),
    );
  });

  it('시스템 항목 순서를 불변 배열로 이동하고 기존 ID를 유지한다', () => {
    const moved = moveEditorItem([optionalItem, existingItem], 1, -1);
    expect(moved.map((item) => item.sourceCheckItemId)).toEqual([
      existingItem.sourceCheckItemId,
      optionalItem.sourceCheckItemId,
    ]);
    expect(moved[0]?.checklistItemId).toBe(existingItem.checklistItemId);
    expect(moveEditorItem(moved, 0, -1)).toBe(moved);
  });
});
