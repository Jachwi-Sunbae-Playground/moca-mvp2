import type { KeyboardEvent } from 'react';
import { checklistStageMeta } from '../constants/checklist';
import { CHECKLIST_STAGES } from '../types/Checklist';
import type { ChecklistStage } from '../types/Checklist';
import { Tab, TabButton, TabList, Tabs } from './ui/Tabs';

type ChecklistStageTabsProps = {
  stage: ChecklistStage;
  getTo?: (stage: ChecklistStage) => string;
  variant?: 'selection' | 'progress';
  onSelect?: (stage: ChecklistStage) => boolean | void | Promise<boolean | void>;
  idPrefix?: string;
  fullBleed?: boolean;
};

const ChecklistStageTabs = ({
  stage,
  getTo = (nextStage) => `/checklists/${nextStage}`,
  variant = 'selection',
  onSelect,
  idPrefix = 'checklist-stage',
  fullBleed,
}: ChecklistStageTabsProps) => {
  const currentIndex = CHECKLIST_STAGES.indexOf(stage);
  const shouldUseFullBleed = fullBleed ?? variant === 'selection';

  const selectFromKeyboard = async (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
    let nextIndex: number | null = null;
    if (event.key === 'ArrowRight') nextIndex = (index + 1) % CHECKLIST_STAGES.length;
    if (event.key === 'ArrowLeft') nextIndex = (index - 1 + CHECKLIST_STAGES.length) % CHECKLIST_STAGES.length;
    if (event.key === 'Home') nextIndex = 0;
    if (event.key === 'End') nextIndex = CHECKLIST_STAGES.length - 1;
    if (nextIndex === null) return;

    event.preventDefault();
    const nextStage = CHECKLIST_STAGES[nextIndex];
    if (nextStage === undefined || onSelect === undefined) return;
    const selected = await onSelect(nextStage);
    if (selected !== false) document.getElementById(`${idPrefix}-${nextStage}`)?.focus();
  };

  return (
    <Tabs
      data-progress-stage={variant === 'progress' ? stage : undefined}
      data-full-bleed={shouldUseFullBleed || undefined}
    >
      <TabList label="체크리스트 단계" role={onSelect === undefined ? undefined : 'tablist'}>
        {CHECKLIST_STAGES.map((item, index) =>
          onSelect === undefined ? (
            <Tab
              key={item}
              to={getTo(item)}
              selected={stage === item}
              data-completed={variant === 'progress' && index <= currentIndex ? 'true' : undefined}
            >
              {checklistStageMeta[item].shortLabel}
            </Tab>
          ) : (
            <TabButton
              key={item}
              id={`${idPrefix}-${item}`}
              selected={stage === item}
              tabIndex={stage === item ? 0 : -1}
              data-completed={variant === 'progress' && index <= currentIndex ? 'true' : undefined}
              onClick={() => void onSelect(item)}
              onKeyDown={(event) => void selectFromKeyboard(event, index)}
            >
              {checklistStageMeta[item].shortLabel}
            </TabButton>
          ),
        )}
      </TabList>
    </Tabs>
  );
};

export default ChecklistStageTabs;
