"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type ISpot, spotService } from "@/entities/spot";

export const useArchiveSpot = (spot: ISpot) => {
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.spots });

  return {
    toggleArchive: useMutation({
      mutationFn: () => (spot.archived ? spotService.restore(spot.id) : spotService.archive(spot.id)),
      onSuccess,
    }),
    removePermanently: useMutation({ mutationFn: () => spotService.deletePermanently(spot.id), onSuccess }),
  };
};
