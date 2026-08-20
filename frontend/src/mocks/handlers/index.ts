import { authHandlers } from './authHandlers';
import { checklistHandlers } from './checklistHandlers';
import { propertyChecklistHandlers } from './propertyChecklistHandlers';
import { propertyHandlers } from './propertyHandlers';

export const handlers = [...authHandlers, ...propertyHandlers, ...checklistHandlers, ...propertyChecklistHandlers];
