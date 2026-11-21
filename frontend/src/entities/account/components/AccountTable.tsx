"use client";

import { ROLE_LABELS } from "@/shared/config";
import { formatDateTime, getTopRole } from "@/shared/lib";
import { Avatar, DataTable, Highlight, type IDataTableColumn, type IDataTableSorting, TableTitleCell } from "@/shared/ui";
import { AccountStatus } from "@/entities/account/components/AccountStatus";
import { ACCOUNT_SORT_FIELDS, ACCOUNT_TEXT, type AccountSortField } from "@/entities/account/constants/account.constant";
import type { IAccount } from "@/entities/account/interfaces/account.interface";

interface IAccountTableProps {
  accounts: IAccount[] | undefined;
  isLoading: boolean;
  search: string;
  emptyText: string;
  sorting: IDataTableSorting<AccountSortField>;
  onSelect: (account: IAccount) => void;
}

const getColumns = (search: string): IDataTableColumn<IAccount, AccountSortField>[] => [
  {
    key: "account",
    header: ACCOUNT_TEXT.account,
    sortField: ACCOUNT_SORT_FIELDS.displayName,
    render: ({ displayName, login }) => (
      <div className="flex items-center gap-3">
        <Avatar name={displayName} size="sm" />
        <TableTitleCell title={<Highlight text={displayName} query={search} />} subtitle={<Highlight text={login} query={search} />} />
      </div>
    ),
  },
  { key: "role", header: ACCOUNT_TEXT.role, render: ({ roles }) => ROLE_LABELS[getTopRole(roles)] },
  { key: "status", header: ACCOUNT_TEXT.status, render: (account) => <AccountStatus {...account} /> },
  { key: "createdAt", header: ACCOUNT_TEXT.createdAt, sortField: ACCOUNT_SORT_FIELDS.createdAt, render: ({ createdAt }) => formatDateTime(createdAt) },
];

export const AccountTable = ({ accounts, isLoading, search, emptyText, sorting, onSelect }: IAccountTableProps) => (
  <DataTable
    columns={getColumns(search)}
    sorting={sorting}
    rows={accounts}
    getRowKey={({ id }) => id}
    isLoading={isLoading}
    emptyText={emptyText}
    onRowClick={onSelect}
  />
);
