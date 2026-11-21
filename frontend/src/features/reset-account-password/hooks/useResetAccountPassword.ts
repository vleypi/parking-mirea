"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { accountService } from "@/entities/account";

export const useResetAccountPassword = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (accountId: string) => accountService.resetPassword(accountId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.accounts }),
  });
};
