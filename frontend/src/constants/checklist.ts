import type { ChecklistPresetType, ChecklistStage } from '../types/Checklist';
import { CHECKLIST_STAGES, PRESET_TYPES } from '../types/Checklist';

export const checklistStageMeta: Record<ChecklistStage, { label: string; shortLabel: string; description: string }> = {
  ONLINE_PHONE: {
    label: '온라인·전화',
    shortLabel: '온라인·전화',
    description: '방문 전에 가격, 위치와 기본 조건을 먼저 확인해요.',
  },
  ON_SITE: {
    label: '집에서 확인',
    shortLabel: '집에서 확인',
    description: '직접 방문해 채광, 소음과 시설 상태를 확인해요.',
  },
  PRE_CONTRACT: {
    label: '계약 전',
    shortLabel: '계약 전',
    description: '서명하기 전에 등기, 특약과 비용을 마지막으로 확인해요.',
  },
};

export const presetMeta: Record<ChecklistPresetType, { label: string; description: string }> = {
  ONE_ROOM: { label: '원룸', description: '일반적인 원룸을 확인할 때 필요한 항목이에요.' },
  GOSHIWON: { label: '고시원', description: '공용 시설과 생활 규칙까지 살펴보는 항목이에요.' },
};

export const isChecklistStage = (value: unknown): value is ChecklistStage =>
  typeof value === 'string' && CHECKLIST_STAGES.includes(value as ChecklistStage);

export const isChecklistPresetType = (value: unknown): value is ChecklistPresetType =>
  typeof value === 'string' && PRESET_TYPES.includes(value as ChecklistPresetType);
