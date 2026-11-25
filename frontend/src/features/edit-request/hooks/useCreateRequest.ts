"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { requestService } from "@/entities/request";
import type { CreateRequestFormValues } from "@/features/edit-request/schemas/create-request-form.schema";
import { toCreatePayload } from "@/features/edit-request/utils/request-form.util";

/** Оформление заявки; после успеха обновляются списки заявок и мест. */
export const useCreateRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (values: CreateRequestFormValues) => requestService.create(toCreatePayload(values)),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.requests });
      void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.spots });
    },
  });
};
