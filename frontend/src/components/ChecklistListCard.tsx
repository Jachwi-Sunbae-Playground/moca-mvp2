import { Link } from 'react-router-dom';
import type { ChecklistSummary } from '../types/Checklist';
import styles from './ChecklistListCard.module.css';

const ChecklistListCard = ({ checklist }: { checklist: ChecklistSummary }) => (
  <li className={styles.card}>
    <Link className={styles.main} to={`/checklists/${checklist.checklistId}`}>
      <strong>{checklist.name}</strong>
      <span>
        {checklist.itemCount}개 항목 · 매물 {checklist.assignedPropertyCount}곳에서 사용
      </span>
    </Link>
    <Link className={styles.edit} to={`/checklists/${checklist.checklistId}`}>
      편집
    </Link>
  </li>
);

export default ChecklistListCard;
