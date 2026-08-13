export type ApiErrorFieldDto = {
  field?: string;
  reason?: string;
};

export type ApiErrorDto = {
  code: string;
  message: string;
  errors: ApiErrorFieldDto[];
};
