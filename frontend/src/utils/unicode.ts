export const getUnicodeCodePointLength = (value: string): number => Array.from(value).length;

export const hasAtMostUnicodeCodePoints = (value: string, maximum: number): boolean =>
  getUnicodeCodePointLength(value) <= maximum;
