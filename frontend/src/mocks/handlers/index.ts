import { authHandlers } from './authHandlers';
import { checklistHandlers } from './checklistHandlers';
import { propertyChecklistHandlers } from './propertyChecklistHandlers';
import { propertyHandlers } from './propertyHandlers';
import { mapHandlers } from './mapHandlers';

export const handlers = [
  ...authHandlers,
  ...propertyHandlers,
  ...checklistHandlers,
  ...propertyChecklistHandlers,
  ...mapHandlers,
];
