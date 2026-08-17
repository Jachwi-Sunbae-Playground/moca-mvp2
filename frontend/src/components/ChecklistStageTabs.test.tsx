import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import ChecklistStageTabs from './ChecklistStageTabs';

describe('체크리스트 단계 탭', () => {
  it('진행형에서는 현재 단계까지 왼쪽부터 완료 상태로 표시한다', () => {
    render(
      <MemoryRouter>
        <ChecklistStageTabs stage="ON_SITE" variant="progress" />
      </MemoryRouter>,
    );

    const tabList = screen.getByRole('navigation', { name: '체크리스트 단계' });
    expect(tabList.parentElement).toHaveAttribute('data-progress-stage', 'ON_SITE');
    expect(screen.getByRole('link', { name: '온라인·전화' })).toHaveAttribute('data-completed', 'true');
    expect(screen.getByRole('link', { name: '집에서 확인' })).toHaveAttribute('data-completed', 'true');
    expect(screen.getByRole('link', { name: '계약 전' })).not.toHaveAttribute('data-completed');
  });

  it('일반 선택형에서는 진행 상태를 표시하지 않는다', () => {
    render(
      <MemoryRouter>
        <ChecklistStageTabs stage="ON_SITE" />
      </MemoryRouter>,
    );

    const tabList = screen.getByRole('navigation', { name: '체크리스트 단계' });
    expect(tabList.parentElement).not.toHaveAttribute('data-progress-stage');
  });
});
