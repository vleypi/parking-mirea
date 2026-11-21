"use client";

import { Button, Icon } from "@/shared/ui";
import { LOGOUT_TEXT } from "@/features/logout/constants/logout.constant";
import { useLogout } from "@/features/logout/hooks/useLogout";

export const LogoutButton = () => {
  const { mutate: logout, isPending } = useLogout();

  return (
    <Button variant="danger" isLoading={isPending} onClick={() => logout()} className="w-full justify-start px-3">
      <Icon name="logout" className="size-4" />
      {LOGOUT_TEXT.logout}
    </Button>
  );
};
