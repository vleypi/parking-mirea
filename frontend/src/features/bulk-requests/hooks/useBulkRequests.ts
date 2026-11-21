"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type IRequestBulkPayload, requestService } from "@/entities/request";

export const useBulkRequests = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: IRequestBulkPayload) => requestService.bulk(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.requests }),
  });
};
