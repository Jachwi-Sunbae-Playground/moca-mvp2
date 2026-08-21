import type { ReactNode } from 'react';
import ChecklistStageTabs from './ChecklistStageTabs';
import TopNavigation from './ui/TopNavigation';
import type { ChecklistStage } from '../types/Checklist';
import styles from './ChecklistPageLayout.module.css';

type ChecklistPageLayoutProps = {
  title: string;
  children: ReactNode;
  backTo?: string;
  onBack?: () => void;
  backLabel?: string;
  navigationIcon?: 'arrow-left' | 'close';
  endSlot?: ReactNode;
  stage?: ChecklistStage;
  getStageTo?: (stage: ChecklistStage) => string;
  className?: string;
  containerClassName?: string;
  tabsClassName?: string;
};

const ChecklistPageLayout = ({
  title,
  children,
  backTo,
  onBack,
  backLabel,
  navigationIcon,
  endSlot,
  stage,
  getStageTo,
  className,
  containerClassName,
  tabsClassName,
}: ChecklistPageLayoutProps) => (
  <main className={`${styles.page} ${className ?? ''}`}>
    <div className={`${styles.container} ${containerClassName ?? ''}`}>
      <TopNavigation
        title={title}
        backTo={backTo}
        onBack={onBack}
        backLabel={backLabel}
        navigationIcon={navigationIcon}
        endSlot={endSlot}
      />
      {stage !== undefined && (
        <div className={`${styles.stageTabs} ${tabsClassName ?? ''}`}>
          <ChecklistStageTabs stage={stage} getTo={getStageTo} fullBleed />
        </div>
      )}
      {children}
    </div>
  </main>
);

export default ChecklistPageLayout;
