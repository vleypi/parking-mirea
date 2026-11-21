"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type IRequest, requestService } from "@/entities/request";

export const useArchiveRequest = (request: IRequest) => {
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.requests });

  return {
    toggleArchive: useMutation({
      mutationFn: () => (request.archived ? requestService.restore(request.id) : requestService.archive(request.id)),
      onSuccess,
    }),
    removePermanently: useMutation({ mutationFn: () => requestService.deletePermanently(request.id), onSuccess }),
  };
};
