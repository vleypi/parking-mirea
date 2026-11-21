import type { IListRequest } from "@/shared/api";
import type { Role } from "@/shared/config";
import type { AccountSortField } from "@/entities/account/constants/account.constant";

export interface IAccount {
  id: string;
  login: string;
  displayName: string;
  roles: Role[];
  enabled: boolean;
  mustChangePassword: boolean;
  lockedUntil?: string;
  createdAt: string;
  version: number;
}

export interface IAccountPayload {
  login: string;
  displayName: string;
  roles: Role[];
}

export interface IAccountUpdatePayload extends IAccountPayload {
  enabled: boolean;
  version: number;
}

export interface ITemporaryPassword {
  user: IAccount;
  temporaryPassword: string;
}

export type IAccountListRequest = IListRequest<AccountSortField>;
