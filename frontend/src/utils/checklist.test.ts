import { describe, expect, it } from 'vitest';
import { onlineItemFixture, secondOnlineItemFixture } from '../test/checklistFixtures';
import type { CheckItem } from '../types/Checklist';
import { hasUniqueSameStageItems, moveChecklistItem, parseChecklistReturnTo, validateChecklistName } from './checklist';

describe('체크리스트 편집 규칙', () => {
  it('이름을 trim 기준 1~50자로 검증한다', () => {
    expect(validateChecklistName('   ')).toBeTruthy();
    expect(validateChecklistName('가'.repeat(51))).toBeTruthy();
    expect(validateChecklistName(' 같은 이름 허용 ')).toBeNull();
  });

  it('같은 단계의 중복되지 않은 항목이 한 개 이상이어야 한다', () => {
    const items = [onlineItemFixture, secondOnlineItemFixture] as CheckItem[];
    expect(hasUniqueSameStageItems(items, 'ONLINE_PHONE')).toBe(true);
    expect(hasUniqueSameStageItems([], 'ONLINE_PHONE')).toBe(false);
    expect(hasUniqueSameStageItems([items[0], items[0]], 'ONLINE_PHONE')).toBe(false);
    expect(hasUniqueSameStageItems(items, 'ON_SITE')).toBe(false);
  });

  it('순서를 불변 배열로 이동하고 경계 밖 이동은 원본을 유지한다', () => {
    const items = [onlineItemFixture, secondOnlineItemFixture] as CheckItem[];
    expect(moveChecklistItem(items, 1, -1).map((item) => item.checkItemId)).toEqual([102, 101]);
    expect(moveChecklistItem(items, 0, -1)).toBe(items);
  });

  it('정확한 매물 활성 체크리스트 내부 경로만 returnTo로 허용한다', () => {
    expect(parseChecklistReturnTo('/properties/10/active-checklists/ONLINE_PHONE')).toEqual({
      propertyId: 10,
      stage: 'ONLINE_PHONE',
      path: '/properties/10/active-checklists/ONLINE_PHONE',
    });
    expect(parseChecklistReturnTo('/properties/10/active-checklists/ON_SITE?from=property-detail')).toEqual({
      propertyId: 10,
      stage: 'ON_SITE',
      path: '/properties/10/active-checklists/ON_SITE?from=property-detail',
    });
    expect(parseChecklistReturnTo('https://evil.example/properties/10/active-checklists/ONLINE_PHONE')).toBeNull();
    expect(parseChecklistReturnTo('//evil.example')).toBeNull();
    expect(parseChecklistReturnTo('/properties/0/active-checklists/ONLINE_PHONE')).toBeNull();
  });
});
