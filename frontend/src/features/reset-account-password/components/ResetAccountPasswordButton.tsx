"use client";

import { useState } from "react";
import { Button, ConfirmModal } from "@/shared/ui";
import type { IAccount, ITemporaryPassword } from "@/entities/account";
import { RESET_ACCOUNT_PASSWORD_TEXT } from "@/features/reset-account-password/constants/reset-account-password.constant";
import { useResetAccountPassword } from "@/features/reset-account-password/hooks/useResetAccountPassword";

interface IResetAccountPasswordButtonProps {
  account: IAccount;
  onReset: (issued: ITemporaryPassword) => void;
}

export const ResetAccountPasswordButton = ({ account, onReset }: IResetAccountPasswordButtonProps) => {
  const [isConfirming, setIsConfirming] = useState(false);
  const { mutate: reset, isPending, error } = useResetAccountPassword();

  const confirm = () =>
    reset(account.id, {
      onSuccess: (issued) => {
        setIsConfirming(false);
        onReset(issued);
      },
    });

  return (
    <>
      <Button variant="ghost" onClick={() => setIsConfirming(true)}>
        {RESET_ACCOUNT_PASSWORD_TEXT.button}
      </Button>
      <ConfirmModal
        isOpen={isConfirming}
        title={RESET_ACCOUNT_PASSWORD_TEXT.title}
        description={RESET_ACCOUNT_PASSWORD_TEXT.description}
        confirmText={RESET_ACCOUNT_PASSWORD_TEXT.confirm}
        isPending={isPending}
        error={error?.message}
        onConfirm={confirm}
        onClose={() => setIsConfirming(false)}
      />
    </>
  );
};
