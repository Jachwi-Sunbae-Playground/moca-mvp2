import { useState } from 'react';
import type { FormEvent } from 'react';
import type { ApiError } from '../apis/apiClient';
import type { PropertyInputDto } from '../apis/dtos/PropertyDto';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import {
  formatMoneyInput,
  propertyFieldErrorMessage,
  toPropertyInputDto,
  validatePropertyForm,
} from '../utils/propertyForm';
import type { PropertyFormErrors, PropertyFormField, PropertyFormValues } from '../utils/propertyForm';
import styles from './PropertyForm.module.css';

type PropertyFormProps = {
  initialValues: PropertyFormValues;
  submitLabel: string;
  isSubmitting: boolean;
  mutationError: ApiError | null;
  formNotice?: string | null;
  onSubmit: (input: PropertyInputDto, values: PropertyFormValues) => void;
};

const fields: PropertyFormField[] = ['name', 'depositAmount', 'monthlyRentAmount', 'discoverySource'];

const PropertyForm = ({
  initialValues,
  submitLabel,
  isSubmitting,
  mutationError,
  formNotice,
  onSubmit,
}: PropertyFormProps) => {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState<PropertyFormErrors>({});

  const displayedErrors = { ...errors };
  mutationError?.invalidFields.forEach((field) => {
    const matchedField = fields.find((candidate) => candidate === field);
    if (matchedField !== undefined && displayedErrors[matchedField] === undefined)
      displayedErrors[matchedField] = propertyFieldErrorMessage(matchedField);
  });

  const setValue = (field: PropertyFormField, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  };

  const setMoneyValue = (field: 'depositAmount' | 'monthlyRentAmount', value: string) => {
    const formatted = formatMoneyInput(value);
    if (formatted !== null) setValue(field, formatted);
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors = validatePropertyForm(values);
    setErrors(nextErrors);

    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    const input = toPropertyInputDto(values);
    if (input !== null) onSubmit(input, values);
  };

  return (
    <form className={styles.form} noValidate onSubmit={handleSubmit}>
      <div className="form-field">
        <label htmlFor="property-name">이름</label>
        <input
          id="property-name"
          name="name"
          value={values.name}
          maxLength={50}
          autoComplete="off"
          aria-invalid={displayedErrors.name !== undefined}
          aria-describedby={displayedErrors.name === undefined ? undefined : 'property-name-error'}
          onChange={(event) => setValue('name', event.target.value)}
        />
        {displayedErrors.name !== undefined && (
          <p id="property-name-error" className="field-error">
            {displayedErrors.name}
          </p>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="property-deposit">보증금</label>
        <div className={styles.moneyInput}>
          <input
            id="property-deposit"
            name="depositAmount"
            value={values.depositAmount}
            inputMode="numeric"
            autoComplete="off"
            aria-invalid={displayedErrors.depositAmount !== undefined}
            aria-describedby={displayedErrors.depositAmount === undefined ? undefined : 'property-deposit-error'}
            onChange={(event) => setMoneyValue('depositAmount', event.target.value)}
          />
          <span aria-hidden="true">원</span>
        </div>
        {displayedErrors.depositAmount !== undefined && (
          <p id="property-deposit-error" className="field-error">
            {displayedErrors.depositAmount}
          </p>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="property-rent">월세</label>
        <div className={styles.moneyInput}>
          <input
            id="property-rent"
            name="monthlyRentAmount"
            value={values.monthlyRentAmount}
            inputMode="numeric"
            autoComplete="off"
            aria-invalid={displayedErrors.monthlyRentAmount !== undefined}
            aria-describedby={displayedErrors.monthlyRentAmount === undefined ? undefined : 'property-rent-error'}
            onChange={(event) => setMoneyValue('monthlyRentAmount', event.target.value)}
          />
          <span aria-hidden="true">원</span>
        </div>
        {displayedErrors.monthlyRentAmount !== undefined && (
          <p id="property-rent-error" className="field-error">
            {displayedErrors.monthlyRentAmount}
          </p>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="property-source">발견 경로</label>
        <textarea
          id="property-source"
          name="discoverySource"
          value={values.discoverySource}
          maxLength={500}
          rows={3}
          aria-invalid={displayedErrors.discoverySource !== undefined}
          aria-describedby={
            displayedErrors.discoverySource === undefined ? 'property-source-help' : 'property-source-error'
          }
          onChange={(event) => setValue('discoverySource', event.target.value)}
        />
        <p id="property-source-help" className="field-help">
          URL과 앱 이름, 중개사 설명 같은 일반 텍스트를 모두 입력할 수 있어요.
        </p>
        {displayedErrors.discoverySource !== undefined && (
          <p id="property-source-error" className="field-error">
            {displayedErrors.discoverySource}
          </p>
        )}
      </div>

      {formNotice !== null && formNotice !== undefined && (
        <p className="form-notice" role="status">
          {formNotice}
        </p>
      )}
      {mutationError !== null && (
        <p className="form-error" role="alert">
          {getPropertyErrorMessage(mutationError)} 입력값은 그대로 유지됩니다.
        </p>
      )}

      <button className={`primary-button ${styles.submit}`} type="submit" disabled={isSubmitting}>
        {isSubmitting ? '저장 중…' : submitLabel}
      </button>
    </form>
  );
};

export default PropertyForm;
