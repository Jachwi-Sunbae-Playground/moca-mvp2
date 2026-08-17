import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import AppBar from './AppBar';
import BottomActionArea from './BottomActionArea';
import EmptyState from './EmptyState';
import InlineNotice from './InlineNotice';
import { Tab, TabButton, TabList, Tabs } from './Tabs';
import TopNavigation from './TopNavigation';

describe('공용 내비게이션과 상태 안내', () => {
  it('AppBar는 제목과 뒤로가기 목적지를 표시한다', () => {
    render(
      <MemoryRouter>
        <AppBar title="새 매물 등록" backTo="/properties" backLabel="매물 목록으로 돌아가기" />
      </MemoryRouter>,
    );

    expect(screen.getByText('새 매물 등록')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '매물 목록으로 돌아가기' })).toHaveAttribute('href', '/properties');
  });

  it('TopNavigation은 뒤로가기 콜백과 우측 슬롯을 제공합니다', async () => {
    const onBack = vi.fn();
    const user = userEvent.setup();

    render(<TopNavigation title="매물 정보" onBack={onBack} endSlot={<button type="button">메뉴</button>} />);

    await user.click(screen.getByRole('button', { name: '뒤로 가기' }));
    expect(onBack).toHaveBeenCalledOnce();
    expect(screen.getByRole('button', { name: '메뉴' })).toBeInTheDocument();
  });

  it('Tab과 TabButton은 현재 선택 상태를 노출합니다', () => {
    render(
      <MemoryRouter>
        <Tabs>
          <TabList label="확인 단계">
            <Tab to="/online" selected>
              온라인·전화
            </Tab>
            <TabButton>집에서 확인</TabButton>
          </TabList>
        </Tabs>
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: '온라인·전화' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('tab', { name: '집에서 확인' })).toHaveAttribute('aria-selected', 'false');
  });

  it('상태 안내와 하단 액션을 의미 있는 영역으로 제공합니다', () => {
    render(
      <>
        <InlineNotice tone="error">목록을 불러오지 못했어요.</InlineNotice>
        <EmptyState title="등록한 매물이 없어요" description="새 매물을 기록해 보세요." />
        <BottomActionArea sticky={false}>저장 영역</BottomActionArea>
      </>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('목록을 불러오지 못했어요.');
    expect(screen.getByText('등록한 매물이 없어요')).toBeInTheDocument();
    expect(screen.getByText('저장 영역')).toBeInTheDocument();
  });
});
