"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type ISpot, spotService } from "@/entities/spot";
import type { SpotFormValues } from "@/features/save-spot/schemas/spot-form.schema";
import { toSpotPayload } from "@/features/save-spot/utils/spot-form.util";

/** Создание или изменение. При изменении уходит version: если запись уже поменяли, сервер ответит 409. */
export const useSaveSpot = (spot: ISpot | null) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (values: SpotFormValues) =>
      spot ? spotService.update(spot.id, { ...toSpotPayload(values), version: spot.version }) : spotService.create(toSpotPayload(values)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.spots }),
  });
};
