import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import ChecklistProgressBar from './ChecklistProgressBar';

describe('ChecklistProgressBar', () => {
  it('괜찮음, 주의, 미확인 결과를 하나의 막대와 범례로 표시한다', () => {
    const { container } = render(
      <ChecklistProgressBar progress={{ goodCount: 2, cautionCount: 1, unconfirmedCount: 1 }} />,
    );

    const summary = screen.getByRole('list', { name: '체크리스트 진행 결과 집계' });
    expect(summary).toHaveTextContent('괜찮음 2');
    expect(summary).toHaveTextContent('주의 1');
    expect(summary).toHaveTextContent('미확인 1');
    expect(container.querySelectorAll('[aria-hidden="true"] > span')).toHaveLength(3);
  });

  it('결과가 0개인 상태는 막대 너비를 차지하지 않고 범례에는 표시한다', () => {
    const { container } = render(
      <ChecklistProgressBar progress={{ goodCount: 0, cautionCount: 2, unconfirmedCount: 1 }} />,
    );

    expect(container.querySelectorAll('[aria-hidden="true"] > span')).toHaveLength(2);
    expect(screen.getByRole('list', { name: '체크리스트 진행 결과 집계' })).toHaveTextContent('괜찮음 0');
  });
});
