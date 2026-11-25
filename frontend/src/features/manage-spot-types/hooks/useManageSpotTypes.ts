"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type ISpotType, referenceService } from "@/entities/reference";
import type { SpotTypeFormValues } from "@/features/manage-spot-types/schemas/spot-type-form.schema";

const toPayload = ({ code, title, sortOrder }: SpotTypeFormValues) => ({
  code: code.toUpperCase(),
  title,
  sortOrder: sortOrder.trim() ? Number(sortOrder) : null,
});

/** После изменения справочника обновляем и сам справочник, и места: у них в ответе название типа. */
const useInvalidate = () => {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.references });
    void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.spots });
  };
};

export const useSaveSpotType = (spotType: ISpotType | null) => {
  const onSuccess = useInvalidate();

  return useMutation({
    mutationFn: (values: SpotTypeFormValues) =>
      spotType ? referenceService.updateSpotType(spotType.id, toPayload(values)) : referenceService.createSpotType(toPayload(values)),
    onSuccess,
  });
};

export const useToggleSpotType = () => {
  const onSuccess = useInvalidate();

  return useMutation({
    mutationFn: (spotType: ISpotType) => referenceService.setSpotTypeActive(spotType.id, !spotType.active),
    onSuccess,
  });
};
