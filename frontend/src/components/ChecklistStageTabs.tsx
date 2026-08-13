import { Link } from 'react-router-dom';
import { checklistStageMeta } from '../constants/checklist';
import { CHECKLIST_STAGES } from '../types/Checklist';
import type { ChecklistStage } from '../types/Checklist';

type ChecklistStageTabsProps = {
  stage: ChecklistStage;
  getTo?: (stage: ChecklistStage) => string;
};

const ChecklistStageTabs = ({ stage, getTo = (nextStage) => `/checklists/${nextStage}` }: ChecklistStageTabsProps) => (
  <nav className="stage-tabs" aria-label="체크리스트 단계">
    {CHECKLIST_STAGES.map((item) => (
      <Link key={item} to={getTo(item)} aria-current={stage === item ? 'page' : undefined}>
        {checklistStageMeta[item].shortLabel}
      </Link>
    ))}
  </nav>
);

export default ChecklistStageTabs;
