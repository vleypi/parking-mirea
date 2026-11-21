"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type IRequest, requestService } from "@/entities/request";
import type { RequestFormValues } from "@/features/edit-request/schemas/request-form.schema";
import { toUpdatePayload } from "@/features/edit-request/utils/request-form.util";

export const useUpdateRequest = (request: IRequest) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (values: RequestFormValues) => requestService.update(request.id, toUpdatePayload(request, values)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.requests }),
  });
};
