import { describe, expect, it } from 'vitest';
import { inlineMemoCodePointLength, isInlineMemoWithinLimit, removeInlineMemoLineBreaks } from './inlineMemo';

describe('방문 인라인 메모 입력 계약', () => {
  it('공백과 빈 문자열을 그대로 보존한다', () => {
    expect(removeInlineMemoLineBreaks('  현관 폭 확인  ')).toBe('  현관 폭 확인  ');
    expect(removeInlineMemoLineBreaks('')).toBe('');
  });

  it('CR과 LF만 제거하고 나머지 문자를 잇는다', () => {
    expect(removeInlineMemoLineBreaks('수압\r\n다시\n확인')).toBe('수압다시확인');
  });

  it('Unicode 코드포인트 기준 200자는 허용하고 201자는 거부한다', () => {
    expect(isInlineMemoWithinLimit('가'.repeat(200))).toBe(true);
    expect(isInlineMemoWithinLimit('가'.repeat(201))).toBe(false);
    expect(inlineMemoCodePointLength('🏠')).toBe(1);
    expect(isInlineMemoWithinLimit('🏠'.repeat(200))).toBe(true);
    expect(isInlineMemoWithinLimit('🏠'.repeat(201))).toBe(false);
  });
});
