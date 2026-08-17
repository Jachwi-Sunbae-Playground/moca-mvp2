import type { PropertyInputDto } from '../apis/dtos/PropertyDto';

export const MAX_PROPERTY_AMOUNT = Number.MAX_SAFE_INTEGER;

export type PropertyFormValues = {
  name: string;
  depositAmount: string;
  monthlyRentAmount: string;
  discoverySource: string;
};

export type PropertyFormField = keyof PropertyFormValues;
export type PropertyFormErrors = Partial<Record<PropertyFormField, string>>;

const countCodePoints = (value: string) => Array.from(value).length;

export const formatMoneyInput = (value: string): string | null => {
  if (value === '') {
    return '';
  }

  if (!/^[0-9,]+$/.test(value)) {
    return null;
  }

  const digits = value.replace(/,/g, '');

  if (digits.length === 0) {
    return null;
  }

  const normalizedDigits = digits.replace(/^0+(?=\d)/, '');
  return normalizedDigits.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
};

export const formatAmountForInput = (amount: number): string => new Intl.NumberFormat('ko-KR').format(amount);

export const parseMoneyInput = (value: string): number | null => {
  const digits = value.replace(/,/g, '');

  if (!/^\d+$/.test(digits)) {
    return null;
  }

  const amount = Number(digits);
  return Number.isSafeInteger(amount) && amount >= 0 && amount <= MAX_PROPERTY_AMOUNT ? amount : null;
};

export const validatePropertyForm = (values: PropertyFormValues): PropertyFormErrors => {
  const errors: PropertyFormErrors = {};
  const name = values.name.trim();
  const discoverySource = values.discoverySource.trim();

  if (name.length === 0) {
    errors.name = '매물을 구분할 이름을 입력해 주세요.';
  } else if (countCodePoints(name) > 50) {
    errors.name = '이름은 50자 이하로 입력해 주세요.';
  }

  if (parseMoneyInput(values.depositAmount) === null) {
    errors.depositAmount = '보증금은 0 이상 최대 안전 정수 이하의 정수로 입력해 주세요.';
  }

  if (parseMoneyInput(values.monthlyRentAmount) === null) {
    errors.monthlyRentAmount = '월세는 0 이상 최대 안전 정수 이하의 정수로 입력해 주세요.';
  }

  if (discoverySource.length === 0) {
    errors.discoverySource = '매물을 확인한 곳을 입력해 주세요.';
  } else if (countCodePoints(discoverySource) > 500) {
    errors.discoverySource = '확인한 곳은 500자 이하로 입력해 주세요.';
  }

  return errors;
};

export const toPropertyInputDto = (values: PropertyFormValues): PropertyInputDto | null => {
  const depositAmount = parseMoneyInput(values.depositAmount);
  const monthlyRentAmount = parseMoneyInput(values.monthlyRentAmount);

  if (depositAmount === null || monthlyRentAmount === null) {
    return null;
  }

  return {
    name: values.name.trim(),
    depositAmount,
    monthlyRentAmount,
    discoverySource: values.discoverySource.trim(),
  };
};

export const propertyFieldErrorMessage = (field: PropertyFormField): string => {
  const messages: Record<PropertyFormField, string> = {
    name: '서버에서 매물 이름을 확인하지 못했습니다.',
    depositAmount: '서버에서 보증금 값을 확인하지 못했습니다.',
    monthlyRentAmount: '서버에서 월세 값을 확인하지 못했습니다.',
    discoverySource: '서버에서 확인한 곳을 확인하지 못했습니다.',
  };

  return messages[field];
};
