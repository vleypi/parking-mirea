"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { QUERY_KEYS } from "@/shared/api";
import type { LoginFormValues } from "@/features/login/schemas/login.schema";
import { loginService } from "@/features/login/services/login.service";
import { resolveRedirect } from "@/features/login/utils/safe-redirect.util";

export const useLogin = (next: string | undefined) => {
  const router = useRouter();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (credentials: LoginFormValues) => loginService.login(credentials),
    onSuccess: (viewer) => {
      queryClient.setQueryData(QUERY_KEYS.viewer, viewer);
      router.replace(resolveRedirect(next));
    },
  });
};
