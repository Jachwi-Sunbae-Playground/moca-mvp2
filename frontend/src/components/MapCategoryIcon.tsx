import type { MapCategory } from '../types/Map';
import styles from './MapCategoryIcon.module.css';

type MapCategoryIconProps = {
  category: MapCategory;
  className?: string;
};

const classNames = (...names: Array<string | undefined>): string => names.filter(Boolean).join(' ');

export const MapCategoryIcon = ({ category, className }: MapCategoryIconProps) => (
  <span className={classNames(styles.icon, className)} data-map-category-icon={category} aria-hidden="true" />
);

export const createMapCategoryIconElement = (category: MapCategory, className?: string): HTMLSpanElement => {
  const element = document.createElement('span');
  element.className = classNames(styles.icon, className);
  element.dataset.mapCategoryIcon = category;
  element.setAttribute('aria-hidden', 'true');
  return element;
};

export default MapCategoryIcon;
