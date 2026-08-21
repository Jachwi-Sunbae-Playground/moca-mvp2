export type PropertyInputDto = {
  name: string;
  depositAmount: number;
  monthlyRentAmount: number;
  maintenanceFeeAmount: number | null;
  discoverySource: string | null;
};

export type UpdatePropertyRequestDto = PropertyInputDto;

export type SavePropertyMemoDocumentRequestDto = {
  items: Array<{
    propertyMemoItemId: number;
    content: string;
  }>;
  freeMemo: string;
};
