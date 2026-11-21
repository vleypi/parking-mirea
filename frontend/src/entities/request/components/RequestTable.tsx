"use client";

import { formatDateTime, formatMoney } from "@/shared/lib";
import type { IRowSelection } from "@/shared/lib";
import { Badge, DataTable, Highlight, type IDataTableColumn, type IDataTableSorting, TableTitleCell } from "@/shared/ui";
import {
  REQUEST_PRIORITY_LABELS,
  REQUEST_PRIORITY_TONES,
  REQUEST_STATUS_TONES,
  REQUEST_SORT_FIELDS,
  REQUEST_TEXT,
  type RequestSortField,
} from "@/entities/request/constants/request.constant";
import type { IRequest } from "@/entities/request/interfaces/request.interface";

interface IRequestTableProps {
  requests: IRequest[] | undefined;
  isLoading: boolean;
  search: string;
  emptyText: string;
  sorting: IDataTableSorting<RequestSortField>;
  selection?: IRowSelection;
  onSelect: (request: IRequest) => void;
}

const getColumns = (search: string): IDataTableColumn<IRequest, RequestSortField>[] => [
  {
    key: "code",
    header: REQUEST_TEXT.code,
    sortField: REQUEST_SORT_FIELDS.code,
    render: ({ code, archived }) => (
      <TableTitleCell title={<Highlight text={code} query={search} />} subtitle={archived ? REQUEST_TEXT.archived : undefined} />
    ),
  },
  {
    key: "owner",
    header: REQUEST_TEXT.owner,
    render: ({ owner }) => <TableTitleCell title={<Highlight text={owner.name} query={search} />} subtitle={owner.phone} />,
  },
  {
    key: "vehicle",
    header: REQUEST_TEXT.vehicle,
    render: ({ vehicle }) => (
      <TableTitleCell title={<Highlight text={vehicle.licensePlate} query={search} />} subtitle={`${vehicle.brand} ${vehicle.model}`} />
    ),
  },
  {
    key: "spot",
    header: REQUEST_TEXT.spot,
    render: ({ spot }) => <TableTitleCell title={`${REQUEST_TEXT.spotNumber} ${spot.number}`} subtitle={spot.typeTitle} />,
  },
  {
    key: "period",
    header: REQUEST_TEXT.period,
    sortField: REQUEST_SORT_FIELDS.startTime,
    render: ({ startTime, endTime }) => <TableTitleCell title={formatDateTime(startTime)} subtitle={formatDateTime(endTime)} />,
  },
  {
    key: "cost",
    header: REQUEST_TEXT.cost,
    sortField: REQUEST_SORT_FIELDS.cost,
    render: ({ cost, paid }) => <TableTitleCell title={formatMoney(cost)} subtitle={paid ? REQUEST_TEXT.paid : REQUEST_TEXT.unpaid} />,
  },
  {
    key: "status",
    header: REQUEST_TEXT.status,
    render: ({ status }) => <Badge tone={REQUEST_STATUS_TONES[status.code]}>{status.title}</Badge>,
  },
  {
    key: "priority",
    header: REQUEST_TEXT.priority,
    render: ({ priority }) => <Badge tone={REQUEST_PRIORITY_TONES[priority]}>{REQUEST_PRIORITY_LABELS[priority]}</Badge>,
  },
];

export const RequestTable = ({ requests, isLoading, search, emptyText, sorting, selection, onSelect }: IRequestTableProps) => (
  <DataTable
    columns={getColumns(search)}
    sorting={sorting}
    rows={requests}
    getRowKey={({ id }) => id}
    isLoading={isLoading}
    emptyText={emptyText}
    selection={selection}
    onRowClick={onSelect}
  />
);
