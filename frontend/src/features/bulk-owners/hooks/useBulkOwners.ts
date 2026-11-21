"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type IOwnerBulkPayload, ownerService } from "@/entities/owner";

export const useBulkOwners = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: IOwnerBulkPayload) => ownerService.bulk(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.owners }),
  });
};
