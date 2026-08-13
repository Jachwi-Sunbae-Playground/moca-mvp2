export const inlineMemoCodePointLength = (value: string): number => Array.from(value).length;

export const removeInlineMemoLineBreaks = (value: string): string => value.replace(/[\r\n]/g, '');

export const isInlineMemoWithinLimit = (value: string): boolean => inlineMemoCodePointLength(value) <= 200;
