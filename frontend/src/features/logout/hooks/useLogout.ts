"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { ROUTES } from "@/shared/config";
import { logoutService } from "@/features/logout/services/logout.service";

export const useLogout = () => {
  const router = useRouter();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => logoutService.logout(),
    onSettled: () => {
      queryClient.clear();
      router.replace(ROUTES.login);
    },
  });
};
