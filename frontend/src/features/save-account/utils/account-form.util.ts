import { ROLES } from "@/shared/config";
import { getTopRole } from "@/shared/lib";
import type { IAccount, IAccountPayload, IAccountUpdatePayload } from "@/entities/account";
import type { AccountFormValues } from "@/features/save-account/schemas/account-form.schema";

export const toAccountFormValues = (account: IAccount | null): AccountFormValues =>
  account
    ? { login: account.login, displayName: account.displayName, role: getTopRole(account.roles), enabled: account.enabled }
    : { login: "", displayName: "", role: ROLES.operator, enabled: true };

export const toCreatePayload = ({ login, displayName, role }: AccountFormValues): IAccountPayload => ({ login, displayName, roles: [role] });

export const toUpdatePayload = (values: AccountFormValues, version: number): IAccountUpdatePayload => ({
  ...toCreatePayload(values),
  enabled: values.enabled,
  version,
});
