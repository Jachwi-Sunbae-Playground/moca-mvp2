import { Link } from 'react-router-dom';
import Icon from '../components/ui/Icon';
import TopNavigation from '../components/ui/TopNavigation';
import { checklistStageMeta } from '../constants/checklist';
import { CHECKLIST_STAGES } from '../types/Checklist';
import styles from './ChecklistHomePage.module.css';

const stageOverviewTitle = {
  ONLINE_PHONE: '온라인·전화 확인',
  ON_SITE: '집에서 확인',
  PRE_CONTRACT: '부동산 계약 확인',
} as const;

const ChecklistHomePage = () => (
  <main className={styles.page}>
    <div className={styles.container}>
      <TopNavigation title="체크리스트" className={styles.navigation} />
      <p className={styles.description}>상황별 기본 체크 세트를 확인하고 내 체크리스트로 구성합니다.</p>
      <ul className={styles.stageList}>
        {CHECKLIST_STAGES.map((stage, index) => {
          const meta = checklistStageMeta[stage];
          return (
            <li key={stage}>
              <Link to={`/checklists/${stage}`}>
                <span className={styles.stageNumber}>{String(index + 1).padStart(2, '0')}</span>
                <span className={styles.stageContent}>
                  <strong>{stageOverviewTitle[stage]}</strong>
                  <small>{meta.description}</small>
                </span>
                <Icon name="arrow-right" size={17} />
              </Link>
            </li>
          );
        })}
      </ul>
    </div>
  </main>
);

export default ChecklistHomePage;
