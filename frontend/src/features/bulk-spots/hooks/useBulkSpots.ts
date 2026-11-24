"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type ISpotBulkPayload, spotService } from "@/entities/spot";

export const useBulkSpots = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: ISpotBulkPayload) => spotService.bulk(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.spots }),
  });
};
