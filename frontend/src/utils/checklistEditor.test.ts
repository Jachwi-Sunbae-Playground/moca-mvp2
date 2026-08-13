import { describe, expect, it } from 'vitest';
import { checkItemToEditorItem, checklistItemToEditorItem, type ChecklistEditorItem } from '../types/ChecklistEditor';
import {
  editorItemsFingerprint,
  moveEditorItem,
  toCreateChecklistItems,
  toUpdateChecklistItems,
  unicodeCodePointLength,
  validateCustomQuestion,
} from './checklistEditor';
import { customChecklistItemFixture, onlineItemFixture, providedChecklistItemFixture } from '../test/checklistFixtures';
import type { CheckItem, ChecklistItem } from '../types/Checklist';

describe('v1.1 체크리스트 편집 상태와 DTO 변환', () => {
  const provided = checkItemToEditorItem(onlineItemFixture as CheckItem);
  const existingCustom = checklistItemToEditorItem(customChecklistItemFixture as ChecklistItem);
  const newCustom: ChecklistEditorItem = {
    clientKey: 'custom:0',
    origin: 'CUSTOM',
    checklistItemId: null,
    sourceCheckItemId: null,
    question: '  환기 상태는 괜찮은가?  ',
    guide: null,
  };

  it('생성 요청은 혼합 순서를 유지하고 로컬 ID나 deprecated 표현을 보내지 않는다', () => {
    const request = toCreateChecklistItems([provided, newCustom]);
    expect(request).toEqual([
      { origin: 'PROVIDED', sourceCheckItemId: 101 },
      { origin: 'CUSTOM', question: '환기 상태는 괜찮은가?' },
    ]);
    expect(request).not.toEqual(
      expect.arrayContaining([expect.objectContaining({ checklistItemId: expect.anything() })]),
    );
  });

  it('수정 요청은 기존 CUSTOM ID만 유지하고 PROVIDED의 두 ID 의미를 섞지 않는다', () => {
    const existingProvided = checklistItemToEditorItem(providedChecklistItemFixture as ChecklistItem);
    expect(toUpdateChecklistItems([existingCustom, existingProvided, newCustom])).toEqual([
      { origin: 'CUSTOM', checklistItemId: 703, question: customChecklistItemFixture.question },
      { origin: 'PROVIDED', sourceCheckItemId: 101 },
      { origin: 'CUSTOM', question: '환기 상태는 괜찮은가?' },
    ]);
  });

  it('같은 문구의 CUSTOM 두 개를 별개 클라이언트 항목으로 보존한다', () => {
    const duplicate = { ...newCustom, clientKey: 'custom:1' };
    expect(toCreateChecklistItems([newCustom, duplicate])).toHaveLength(2);
    expect(editorItemsFingerprint([newCustom, duplicate])).not.toBe(editorItemsFingerprint([newCustom]));
  });

  it('질문을 trim한 Unicode 코드포인트 1~200자로 검증하고 이모지는 한 글자로 센다', () => {
    expect(validateCustomQuestion('가')).toBeNull();
    expect(validateCustomQuestion('가'.repeat(200))).toBeNull();
    expect(validateCustomQuestion('가'.repeat(201))).toContain('200자');
    expect(validateCustomQuestion('   \n ')).toContain('입력');
    expect(unicodeCodePointLength('🏠')).toBe(1);
    expect(validateCustomQuestion('🏠'.repeat(200))).toBeNull();
    expect(validateCustomQuestion('🏠'.repeat(201))).toContain('200자');
  });

  it('혼합 순서를 불변 배열로 이동하고 기존 로컬 ID를 그대로 둔다', () => {
    const moved = moveEditorItem([provided, existingCustom], 1, -1);
    expect(moved.map((item) => item.origin)).toEqual(['CUSTOM', 'PROVIDED']);
    expect(moved[0]?.checklistItemId).toBe(703);
    expect(moveEditorItem(moved, 0, -1)).toBe(moved);
  });
});
