import type { MapCategory } from '../types/Map';
import MapCategoryIcon from './MapCategoryIcon';
import { MAP_CATEGORY_OPTIONS } from './mapPresentation';
import styles from './MapCategoryRail.module.css';

type MapCategoryRailProps = {
  selectedCategories: MapCategory[];
  counts?: Partial<Record<MapCategory, number>>;
  onToggle: (category: MapCategory) => void;
};

const MapCategoryRail = ({ selectedCategories, counts, onToggle }: MapCategoryRailProps) => (
  <aside className={styles.rail} aria-label="지도 시설 카테고리">
    {MAP_CATEGORY_OPTIONS.map((option) => {
      const selected = selectedCategories.includes(option.value);
      const count = counts?.[option.value];
      return (
        <button
          key={option.value}
          type="button"
          className={styles.categoryButton}
          data-category={option.value}
          aria-pressed={selected}
          aria-label={`${option.label} ${selected ? '숨기기' : '표시하기'}${count === undefined ? '' : `, ${count}개`}`}
          onClick={() => onToggle(option.value)}
        >
          <MapCategoryIcon category={option.value} className={styles.icon} />
          <span>{option.shortLabel}</span>
        </button>
      );
    })}
  </aside>
);

export default MapCategoryRail;
