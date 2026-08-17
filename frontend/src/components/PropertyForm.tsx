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
import BottomActionArea from './ui/BottomActionArea';
import { Button } from './ui/Button';
import InlineNotice from './ui/InlineNotice';
import TextAreaField from './ui/TextAreaField';
import TextField from './ui/TextField';
import styles from './PropertyForm.module.css';

type PropertyFormProps = {
  initialValues: PropertyFormValues;
  submitLabel: string;
  isSubmitting: boolean;
  mutationError: ApiError | null;
  formNotice?: string | null;
  variant?: 'default' | 'detail';
  onSubmit: (input: PropertyInputDto, values: PropertyFormValues) => void;
};

const fields: PropertyFormField[] = ['name', 'depositAmount', 'monthlyRentAmount', 'discoverySource'];

const PropertyForm = ({
  initialValues,
  submitLabel,
  isSubmitting,
  mutationError,
  formNotice,
  variant = 'default',
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

    if (Object.keys(nextErrors).length > 0) return;

    const input = toPropertyInputDto(values);
    if (input !== null) onSubmit(input, values);
  };

  return (
    <form
      className={`${styles.form} ${variant === 'detail' ? styles.detailForm : ''}`}
      noValidate
      onSubmit={handleSubmit}
    >
      <TextField
        id="property-name"
        name="name"
        label="이름"
        value={values.name}
        maxLength={50}
        autoComplete="off"
        placeholder="예: 신림역 근처 원룸"
        fieldClassName={variant === 'detail' ? styles.detailField : styles.defaultField}
        className={variant === 'detail' ? styles.detailInput : styles.defaultInput}
        error={displayedErrors.name}
        onChange={(event) => setValue('name', event.target.value)}
      />

      <div className={variant === 'detail' ? styles.detailMoneyFields : styles.moneyFields}>
        <TextField
          id="property-deposit"
          name="depositAmount"
          label="보증금"
          value={values.depositAmount}
          inputMode="numeric"
          autoComplete="off"
          placeholder="0"
          suffix="원"
          fieldClassName={variant === 'detail' ? styles.detailField : styles.defaultField}
          className={variant === 'detail' ? styles.detailInput : styles.defaultInput}
          error={displayedErrors.depositAmount}
          onChange={(event) => setMoneyValue('depositAmount', event.target.value)}
        />

        <TextField
          id="property-rent"
          name="monthlyRentAmount"
          label="월세"
          value={values.monthlyRentAmount}
          inputMode="numeric"
          autoComplete="off"
          placeholder="0"
          suffix="원"
          fieldClassName={variant === 'detail' ? styles.detailField : styles.defaultField}
          className={variant === 'detail' ? styles.detailInput : styles.defaultInput}
          error={displayedErrors.monthlyRentAmount}
          onChange={(event) => setMoneyValue('monthlyRentAmount', event.target.value)}
        />
      </div>

      {variant === 'detail' ? (
        <TextAreaField
          id="property-source"
          name="discoverySource"
          label="확인한 곳"
          value={values.discoverySource}
          maxLength={500}
          rows={3}
          placeholder="URL, 앱 이름 또는 중개사 정보를 입력해 주세요."
          fieldClassName={styles.detailField}
          className={styles.detailInput}
          error={displayedErrors.discoverySource}
          onChange={(event) => setValue('discoverySource', event.target.value)}
        />
      ) : (
        <TextField
          id="property-source"
          name="discoverySource"
          label="확인한 곳"
          value={values.discoverySource}
          maxLength={500}
          autoComplete="off"
          placeholder="URL, 앱 이름 또는 중개사 정보"
          fieldClassName={styles.defaultField}
          className={styles.defaultInput}
          error={displayedErrors.discoverySource}
          onChange={(event) => setValue('discoverySource', event.target.value)}
        />
      )}

      {formNotice !== null && formNotice !== undefined && <InlineNotice>{formNotice}</InlineNotice>}
      {mutationError !== null && (
        <InlineNotice tone="error">{getPropertyErrorMessage(mutationError)} 입력값은 그대로 유지됩니다.</InlineNotice>
      )}

      <BottomActionArea>
        <Button type="submit" variant="soft" fullWidth isLoading={isSubmitting} loadingLabel="저장 중…">
          {submitLabel}
        </Button>
      </BottomActionArea>
    </form>
  );
};

export default PropertyForm;
