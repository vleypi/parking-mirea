"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { accountService, type IAccount, type ITemporaryPassword } from "@/entities/account";
import type { AccountFormValues } from "@/features/save-account/schemas/account-form.schema";
import { toCreatePayload, toUpdatePayload } from "@/features/save-account/utils/account-form.util";

export const useSaveAccount = (account: IAccount | null) => {
  const queryClient = useQueryClient();

  return useMutation<IAccount | ITemporaryPassword, Error, AccountFormValues>({
    mutationFn: (values) =>
      account ? accountService.update(account.id, toUpdatePayload(values, account.version)) : accountService.create(toCreatePayload(values)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.accounts }),
  });
};
