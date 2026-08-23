export type PropertyInputDto = {
  name: string;
  depositAmount?: number;
  monthlyRentAmount?: number;
  discoverySource: string | null;
  roadAddress?: string | null;
  jibunAddress?: string | null;
  latitude?: number | null;
  longitude?: number | null;
};

export type UpdatePropertyRequestDto = PropertyInputDto;

export type SavePropertyMemoDocumentRequestDto = {
  items: Array<{
    systemMemoItemId: number;
    content: string;
  }>;
  freeMemo: string;
};
