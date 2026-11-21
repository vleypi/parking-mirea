"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import type { ChangePasswordFormValues } from "@/features/change-password/schemas/change-password.schema";
import { changePasswordService } from "@/features/change-password/services/change-password.service";

export const useChangePassword = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (values: ChangePasswordFormValues) => changePasswordService.change(values),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.viewer }),
  });
};
