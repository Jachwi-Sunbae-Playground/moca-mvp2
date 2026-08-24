import type { MapCategory } from '../types/Map';

export const MAP_CATEGORY_OPTIONS: Array<{ value: MapCategory; label: string; shortLabel: string }> = [
  { value: 'HOSPITAL', label: '병원', shortLabel: '병원' },
  { value: 'TRANSPORT', label: '교통', shortLabel: '교통' },
  { value: 'SCHOOL', label: '학교', shortLabel: '학교' },
  { value: 'CONVENIENCE', label: '편의점', shortLabel: '편의' },
  { value: 'AGENCY', label: '중개업소', shortLabel: '중개' },
];

export const ALL_MAP_CATEGORIES = MAP_CATEGORY_OPTIONS.map((option) => option.value);

export const getMapCategoryLabel = (category: MapCategory): string =>
  MAP_CATEGORY_OPTIONS.find((option) => option.value === category)?.label ?? category;
