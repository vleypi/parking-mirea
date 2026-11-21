import { Badge } from "@/shared/ui";
import { ACCOUNT_TEXT } from "@/entities/account/constants/account.constant";
import type { IAccount } from "@/entities/account/interfaces/account.interface";

type IAccountStatusProps = Pick<IAccount, "enabled" | "mustChangePassword" | "lockedUntil">;

const isLocked = (lockedUntil: string | undefined): boolean => Boolean(lockedUntil && new Date(lockedUntil) > new Date());

export const AccountStatus = ({ enabled, mustChangePassword, lockedUntil }: IAccountStatusProps) => {
  if (!enabled) return <Badge tone="danger">{ACCOUNT_TEXT.disabled}</Badge>;
  if (isLocked(lockedUntil)) return <Badge tone="warning">{ACCOUNT_TEXT.locked}</Badge>;
  if (mustChangePassword) return <Badge tone="brand">{ACCOUNT_TEXT.mustChangePassword}</Badge>;
  return <Badge tone="success">{ACCOUNT_TEXT.active}</Badge>;
};
