import { useMutation } from '@tanstack/react-query';
import { ApiError } from '../../apis/apiClient';
import { completeVisit, startPropertyVisit } from '../../apis/visitApi';
import { propertyQueryKeys } from '../../app/propertyQueryKeys';
import { queryClient } from '../../app/queryClient';
import { applyVisitCompletion, invalidateVisitAggregates } from '../../app/visitCache';
import { visitQueryKeys } from '../../app/visitQueryKeys';
import type { PublicConfig } from '../../types/PublicConfig';
import type { VisitDetail } from '../../types/Visit';

export const useStartPropertyVisit = (config: PublicConfig, propertyId: number) =>
  useMutation({
    retry: false,
    mutationFn: async () => {
      const visit = await startPropertyVisit(config, propertyId);
      if (visit.propertyId !== propertyId) throw new ApiError({ kind: 'invalid-response' });
      return visit;
    },
    onSuccess: (visit) => {
      queryClient.setQueryData(visitQueryKeys.detail(visit.visitId), visit);
      void invalidateVisitAggregates(queryClient, propertyId);
    },
  });

export const useCompleteVisit = (config: PublicConfig, detail: VisitDetail) =>
  useMutation({
    retry: false,
    mutationFn: () => completeVisit(config, detail.visitId),
    onSuccess: (result) => {
      queryClient.setQueryData<VisitDetail>(visitQueryKeys.detail(detail.visitId), (current) =>
        applyVisitCompletion(current, result),
      );
      void invalidateVisitAggregates(queryClient, detail.propertyId);
    },
  });

export const invalidatePropertyAfterVisitFailure = (propertyId: number) =>
  queryClient.invalidateQueries({ queryKey: propertyQueryKeys.detail(propertyId), exact: true });
