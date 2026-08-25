import type { ChecklistItemInputDto } from '../apis/dtos/ChecklistDto';
import type { ChecklistEditorItem } from '../types/ChecklistEditor';

export const unicodeCodePointLength = (value: string): number => Array.from(value).length;

export const validateCustomQuestion = (value: string): string | null => {
  const question = value.trim();
  if (unicodeCodePointLength(question) === 0) return '직접 추가할 질문을 입력해 주세요.';
  if (unicodeCodePointLength(question) > 200) return '직접 추가 질문은 200자 이하로 입력해 주세요.';
  return null;
};

export const moveEditorItem = (
  items: ChecklistEditorItem[],
  index: number,
  direction: -1 | 1,
): ChecklistEditorItem[] => {
  const destination = index + direction;
  if (index < 0 || index >= items.length || destination < 0 || destination >= items.length) return items;
  const result = [...items];
  [result[index], result[destination]] = [result[destination], result[index]];
  return result;
};

export const editorItemsFingerprint = (items: ChecklistEditorItem[]): string =>
  JSON.stringify(
    items.map((item) =>
      item.origin === 'PROVIDED'
        ? ['PROVIDED', item.checklistItemId, item.sourceCheckItemId]
        : ['CUSTOM', item.checklistItemId, item.question],
    ),
  );

export const toChecklistItemInputs = (items: ChecklistEditorItem[]): ChecklistItemInputDto[] =>
  items.map((item) =>
    item.origin === 'PROVIDED'
      ? { systemCheckItemId: item.sourceCheckItemId }
      : { systemCheckItemId: null, question: item.question.trim() },
  );
