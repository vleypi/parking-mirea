"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type IOwner, ownerService } from "@/entities/owner";

export const useArchiveOwner = (owner: IOwner) => {
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.owners });

  return {
    toggleArchive: useMutation({
      mutationFn: () => (owner.archived ? ownerService.restore(owner.id) : ownerService.archive(owner.id)),
      onSuccess,
    }),
    removePermanently: useMutation({ mutationFn: () => ownerService.deletePermanently(owner.id), onSuccess }),
  };
};
