"use client";

import { formatDateTime } from "@/shared/lib";
import type { IRowSelection } from "@/shared/lib";
import { Badge, DataTable, Highlight, type IDataTableColumn, type IDataTableSorting, TableTitleCell } from "@/shared/ui";
import { LOYALTY_TONES, OWNER_SORT_FIELDS, OWNER_STATUS_TONES, OWNER_TEXT, type OwnerSortField } from "@/entities/owner/constants/owner.constant";
import type { IOwner } from "@/entities/owner/interfaces/owner.interface";

interface IOwnerTableProps {
  owners: IOwner[] | undefined;
  isLoading: boolean;
  search: string;
  emptyText: string;
  sorting: IDataTableSorting<OwnerSortField>;
  selection?: IRowSelection;
  onSelect: (owner: IOwner) => void;
}

const getColumns = (search: string): IDataTableColumn<IOwner, OwnerSortField>[] => [
  {
    key: "owner",
    header: OWNER_TEXT.owner,
    sortField: OWNER_SORT_FIELDS.name,
    render: ({ name, archived }) => (
      <TableTitleCell title={<Highlight text={name} query={search} />} subtitle={archived ? OWNER_TEXT.archived : undefined} />
    ),
  },
  {
    key: "contacts",
    header: OWNER_TEXT.contacts,
    render: ({ phone, email }) => (
      <TableTitleCell title={<Highlight text={phone} query={search} />} subtitle={email ? <Highlight text={email} query={search} /> : undefined} />
    ),
  },
  { key: "category", header: OWNER_TEXT.category, render: ({ category }) => category.title },
  {
    key: "status",
    header: OWNER_TEXT.status,
    render: ({ status }) => <Badge tone={OWNER_STATUS_TONES[status.code]}>{status.title}</Badge>,
  },
  {
    key: "loyalty",
    header: OWNER_TEXT.loyalty,
    render: ({ loyalty, completedRequests }) => (
      <span className="flex items-center gap-2">
        <Badge tone={LOYALTY_TONES[loyalty.code]}>{loyalty.title}</Badge>
        <span className="text-xs text-gray-500 tabular-nums">{completedRequests}</span>
      </span>
    ),
  },
  {
    key: "discount",
    header: OWNER_TEXT.discount,
    sortField: OWNER_SORT_FIELDS.discount,
    render: ({ discountPercent }) => <span className="tabular-nums">{discountPercent} %</span>,
  },
  { key: "vehicles", header: OWNER_TEXT.vehicles, render: ({ vehiclesCount }) => <span className="tabular-nums">{vehiclesCount}</span> },
  {
    key: "createdAt",
    header: OWNER_TEXT.createdAt,
    sortField: OWNER_SORT_FIELDS.createdAt,
    render: ({ createdAt }) => <span className="whitespace-nowrap">{formatDateTime(createdAt)}</span>,
  },
];

export const OwnerTable = ({ owners, isLoading, search, emptyText, sorting, selection, onSelect }: IOwnerTableProps) => (
  <DataTable
    columns={getColumns(search)}
    sorting={sorting}
    rows={owners}
    getRowKey={({ id }) => id}
    isLoading={isLoading}
    emptyText={emptyText}
    selection={selection}
    onRowClick={onSelect}
  />
);
