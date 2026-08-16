import { Link } from 'react-router-dom';
import PageHeading from '../components/PageHeading';
import { checklistStageMeta } from '../constants/checklist';
import { CHECKLIST_STAGES } from '../types/Checklist';
import styles from './ChecklistHomePage.module.css';

const ChecklistHomePage = () => (
  <main className="property-page">
    <div className={`page-container ${styles.container}`}>
      <PageHeading title="체크리스트" description="상황에 맞는 확인 목록을 만들고 여러 매물에서 다시 사용해 보세요." />
      <div className={styles.cardList}>
        {CHECKLIST_STAGES.map((stage, index) => (
          <Link key={stage} className={styles.card} to={`/checklists/${stage}`}>
            <span className={styles.number}>{index + 1}</span>
            <span>
              <strong>{checklistStageMeta[stage].label}</strong>
              <small>{checklistStageMeta[stage].description}</small>
            </span>
            <span aria-hidden="true">→</span>
          </Link>
        ))}
      </div>
      <p className="section-note">단계마다 필요한 항목만 담을 수 있으며, 만든 뒤에는 단계를 변경할 수 없어요.</p>
    </div>
  </main>
);

export default ChecklistHomePage;
