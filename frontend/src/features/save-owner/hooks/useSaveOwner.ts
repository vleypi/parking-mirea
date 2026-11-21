"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type IOwner, ownerService } from "@/entities/owner";
import type { OwnerFormValues } from "@/features/save-owner/schemas/owner-form.schema";
import { toOwnerPayload } from "@/features/save-owner/utils/owner-form.util";

/** Создание или изменение. При изменении уходит version: если запись уже поменяли, сервер ответит 409. */
export const useSaveOwner = (owner: IOwner | null) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (values: OwnerFormValues) =>
      owner ? ownerService.update(owner.id, { ...toOwnerPayload(values), version: owner.version }) : ownerService.create(toOwnerPayload(values)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.owners }),
  });
};
