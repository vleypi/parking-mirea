"use client";

import { formatDateTime } from "@/shared/lib";
import type { IRowSelection } from "@/shared/lib";
import { Badge, DataTable, Highlight, type IDataTableColumn, type IDataTableSorting, TableTitleCell } from "@/shared/ui";
import {
  VEHICLE_ACTIVITY_TONES,
  VEHICLE_SORT_FIELDS,
  VEHICLE_STATUS_TONES,
  VEHICLE_TEXT,
  type VehicleSortField,
} from "@/entities/vehicle/constants/vehicle.constant";
import type { IVehicle } from "@/entities/vehicle/interfaces/vehicle.interface";

interface IVehicleTableProps {
  vehicles: IVehicle[] | undefined;
  isLoading: boolean;
  search: string;
  emptyText: string;
  sorting?: IDataTableSorting<VehicleSortField>;
  selection?: IRowSelection;
  onSelect: (vehicle: IVehicle) => void;
}

const getColumns = (search: string): IDataTableColumn<IVehicle, VehicleSortField>[] => [
  {
    key: "licensePlate",
    header: VEHICLE_TEXT.licensePlate,
    sortField: VEHICLE_SORT_FIELDS.licensePlate,
    render: ({ licensePlate, archived }) => (
      <TableTitleCell title={<Highlight text={licensePlate} query={search} />} subtitle={archived ? VEHICLE_TEXT.archived : undefined} />
    ),
  },
  {
    key: "vehicle",
    header: VEHICLE_TEXT.vehicle,
    sortField: VEHICLE_SORT_FIELDS.brand,
    render: ({ brand, model, electric }) => (
      <TableTitleCell title={<Highlight text={`${brand} ${model}`} query={search} />} subtitle={electric ? VEHICLE_TEXT.electric : undefined} />
    ),
  },
  {
    key: "owner",
    header: VEHICLE_TEXT.owner,
    render: ({ owner }) => <TableTitleCell title={owner.name} subtitle={owner.archived ? VEHICLE_TEXT.ownerArchived : owner.phone} />,
  },
  {
    key: "year",
    header: VEHICLE_TEXT.year,
    sortField: VEHICLE_SORT_FIELDS.releaseYear,
    render: ({ releaseYear }) => <span className="tabular-nums">{releaseYear ?? "—"}</span>,
  },
  { key: "status", header: VEHICLE_TEXT.status, render: ({ status }) => <Badge tone={VEHICLE_STATUS_TONES[status.code]}>{status.title}</Badge> },
  {
    key: "activity",
    header: VEHICLE_TEXT.activity,
    render: ({ activity, recentRequests }) => (
      <span className="flex items-center gap-2">
        <Badge tone={VEHICLE_ACTIVITY_TONES[activity.code]}>{activity.title}</Badge>
        <span className="text-xs text-gray-500 tabular-nums">{recentRequests}</span>
      </span>
    ),
  },
  {
    key: "createdAt",
    header: VEHICLE_TEXT.createdAt,
    sortField: VEHICLE_SORT_FIELDS.createdAt,
    render: ({ createdAt }) => <span className="whitespace-nowrap">{formatDateTime(createdAt)}</span>,
  },
];

export const VehicleTable = ({ vehicles, isLoading, search, emptyText, sorting, selection, onSelect }: IVehicleTableProps) => (
  <DataTable
    columns={getColumns(search)}
    sorting={sorting}
    rows={vehicles}
    getRowKey={({ id }) => id}
    isLoading={isLoading}
    emptyText={emptyText}
    selection={selection}
    onRowClick={onSelect}
  />
);
