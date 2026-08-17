import { Button } from './ui/Button';
import styles from './ChecklistStartOptions.module.css';

export type ChecklistStartMode = 'EMPTY' | 'ONE_ROOM';

type ChecklistStartOptionsProps = {
  onSelect: (mode: ChecklistStartMode) => void;
};

const ChecklistStartOptions = ({ onSelect }: ChecklistStartOptionsProps) => (
  <div className={styles.options}>
    <Button aria-label="빈 목록으로 시작" variant="secondary" fullWidth type="button" onClick={() => onSelect('EMPTY')}>
      <span className={styles.optionContent}>
        <strong>빈 목록</strong>
        <span aria-hidden="true">&gt;</span>
      </span>
    </Button>
    <Button
      aria-label="원룸 제공 항목으로 시작"
      variant="secondary"
      fullWidth
      type="button"
      onClick={() => onSelect('ONE_ROOM')}
    >
      <span className={styles.optionContent}>
        <strong>원룸 제공 항목</strong>
        <span aria-hidden="true">&gt;</span>
      </span>
    </Button>
  </div>
);

export default ChecklistStartOptions;
