export type PropertyInputDto = {
  name: string;
  depositAmount: number;
  monthlyRentAmount: number;
  discoverySource: string;
};

export type UpdatePropertyRequestDto = Partial<PropertyInputDto>;

export type SavePropertyPreVisitMemoRequestDto = {
  viewingSchedule: string;
  moveInAvailability: string;
  provisionalDeposit: string;
  roomOptions: string;
  maintenanceAndUtilities: string;
  commuteTime: string;
  governmentSupport: string;
  additionalMemo: string;
  content?: never;
};

/** @deprecated v1.0 화면 호환 전용이다. 신규 코드는 SavePropertyPreVisitMemoRequestDto를 사용한다. */
export type SavePropertyMemoRequestDto = {
  content: string;
  viewingSchedule?: never;
  moveInAvailability?: never;
  provisionalDeposit?: never;
  roomOptions?: never;
  maintenanceAndUtilities?: never;
  commuteTime?: never;
  governmentSupport?: never;
  additionalMemo?: never;
};
