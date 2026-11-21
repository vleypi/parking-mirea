"use client";

import { useState } from "react";
import { Button } from "@/shared/ui";
import { ACCOUNT_TEXT } from "@/entities/account/constants/account.constant";

interface ITemporaryPasswordNoticeProps {
  login: string;
  password: string;
}

export const TemporaryPasswordNotice = ({ login, password }: ITemporaryPasswordNoticeProps) => {
  const [isCopied, setIsCopied] = useState(false);

  const copy = async () => {
    await navigator.clipboard.writeText(password);
    setIsCopied(true);
  };

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-gray-600">{ACCOUNT_TEXT.temporaryPasswordHint}</p>
      <div className="flex items-center justify-between gap-3 rounded-xl bg-brand-soft px-4 py-3">
        <div className="min-w-0">
          <p className="truncate text-xs text-gray-500">{login}</p>
          <p className="font-mono text-lg font-semibold tracking-wider text-ink select-all">{password}</p>
        </div>
        <Button variant="secondary" onClick={copy}>
          {isCopied ? ACCOUNT_TEXT.copied : ACCOUNT_TEXT.copy}
        </Button>
      </div>
    </div>
  );
};
