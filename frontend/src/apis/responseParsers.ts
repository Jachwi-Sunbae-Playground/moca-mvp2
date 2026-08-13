import { hasAtMostUnicodeCodePoints } from '../utils/unicode';

const UTC_DATE_TIME_PATTERN =
  /^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d{1,9})?Z$/;

const hasOwn = (record: Record<string, unknown>, key: string): boolean =>
  Object.prototype.hasOwnProperty.call(record, key);

export const readRecord = (value: unknown): Record<string, unknown> => {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error('객체 응답이 필요합니다.');
  }

  return value as Record<string, unknown>;
};

export const readString = (
  record: Record<string, unknown>,
  key: string,
  { allowEmpty = false, maximumCodePoints }: { allowEmpty?: boolean; maximumCodePoints?: number } = {},
): string => {
  if (!hasOwn(record, key)) throw new Error(`${key} 필드가 필요합니다.`);

  const value = record[key];
  if (
    typeof value !== 'string' ||
    (!allowEmpty && value.length === 0) ||
    (maximumCodePoints !== undefined && !hasAtMostUnicodeCodePoints(value, maximumCodePoints))
  ) {
    throw new Error(`${key} 문자열 응답이 올바르지 않습니다.`);
  }

  return value;
};

export const readNullableString = (
  record: Record<string, unknown>,
  key: string,
  options?: { allowEmpty?: boolean; maximumCodePoints?: number },
): string | null => {
  if (!hasOwn(record, key)) throw new Error(`${key} 필드가 필요합니다.`);
  return record[key] === null ? null : readString(record, key, options);
};

export const readInteger = (record: Record<string, unknown>, key: string, minimum = 0): number => {
  if (!hasOwn(record, key)) throw new Error(`${key} 필드가 필요합니다.`);

  const value = record[key];
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < minimum) {
    throw new Error(`${key} 정수 응답이 올바르지 않습니다.`);
  }

  return value;
};

export const readNullableInteger = (record: Record<string, unknown>, key: string, minimum = 0): number | null => {
  if (!hasOwn(record, key)) throw new Error(`${key} 필드가 필요합니다.`);
  return record[key] === null ? null : readInteger(record, key, minimum);
};

export const readBoolean = (record: Record<string, unknown>, key: string): boolean => {
  if (!hasOwn(record, key) || typeof record[key] !== 'boolean') {
    throw new Error(`${key} boolean 응답이 올바르지 않습니다.`);
  }

  return record[key];
};

export const readArray = (record: Record<string, unknown>, key: string): unknown[] => {
  if (!hasOwn(record, key) || !Array.isArray(record[key])) {
    throw new Error(`${key} 배열 응답이 올바르지 않습니다.`);
  }

  return record[key];
};

export const readUtcDateTime = (record: Record<string, unknown>, key: string): string => {
  const value = readString(record, key);
  const timestamp = Date.parse(value);
  if (
    !UTC_DATE_TIME_PATTERN.test(value) ||
    Number.isNaN(timestamp) ||
    new Date(timestamp).toISOString().slice(0, 19) !== value.slice(0, 19)
  ) {
    throw new Error(`${key} UTC date-time 응답이 올바르지 않습니다.`);
  }

  return value;
};

export const readNullableUtcDateTime = (record: Record<string, unknown>, key: string): string | null => {
  if (!hasOwn(record, key)) throw new Error(`${key} 필드가 필요합니다.`);
  return record[key] === null ? null : readUtcDateTime(record, key);
};
