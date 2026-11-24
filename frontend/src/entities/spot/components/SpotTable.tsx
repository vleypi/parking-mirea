"use client";

import { formatMoney } from "@/shared/lib";
import type { IRowSelection } from "@/shared/lib";
import { Badge, DataTable, Highlight, type IDataTableColumn, type IDataTableSorting, TableTitleCell } from "@/shared/ui";
import {
  SPOT_AVAILABILITY_TONES,
  SPOT_SORT_FIELDS,
  SPOT_STATUS_TONES,
  SPOT_TEXT,
  type SpotSortField,
} from "@/entities/spot/constants/spot.constant";
import type { ISpot } from "@/entities/spot/interfaces/spot.interface";

interface ISpotTableProps {
  spots: ISpot[] | undefined;
  isLoading: boolean;
  search: string;
  emptyText: string;
  sorting?: IDataTableSorting<SpotSortField>;
  selection?: IRowSelection;
  onSelect: (spot: ISpot) => void;
}

const getColumns = (search: string): IDataTableColumn<ISpot, SpotSortField>[] => [
  {
    key: "spotNumber",
    header: SPOT_TEXT.spotNumber,
    sortField: SPOT_SORT_FIELDS.spotNumber,
    render: ({ spotNumber, archived }) => (
      <TableTitleCell title={<Highlight text={String(spotNumber)} query={search} />} subtitle={archived ? SPOT_TEXT.archived : undefined} />
    ),
  },
  {
    key: "type",
    header: SPOT_TEXT.type,
    render: ({ spotType, covered }) => (
      <TableTitleCell title={spotType.title} subtitle={spotType.active ? (covered ? SPOT_TEXT.covered : undefined) : SPOT_TEXT.typeDisabled} />
    ),
  },
  {
    key: "zone",
    header: SPOT_TEXT.zone,
    sortField: SPOT_SORT_FIELDS.zone,
    render: ({ zone }) => (zone ? <Highlight text={zone} query={search} /> : <span className="text-gray-400">—</span>),
  },
  {
    key: "rate",
    header: SPOT_TEXT.rate,
    sortField: SPOT_SORT_FIELDS.hourlyRate,
    render: ({ hourlyRate }) => (
      <span className="whitespace-nowrap tabular-nums">
        {formatMoney(hourlyRate)}
        {SPOT_TEXT.perHourShort}
      </span>
    ),
  },
  { key: "status", header: SPOT_TEXT.status, render: ({ status }) => <Badge tone={SPOT_STATUS_TONES[status.code]}>{status.title}</Badge> },
  {
    key: "availability",
    header: SPOT_TEXT.availability,
    render: ({ availability }) => <Badge tone={SPOT_AVAILABILITY_TONES[availability.code]}>{availability.title}</Badge>,
  },
];

export const SpotTable = ({ spots, isLoading, search, emptyText, sorting, selection, onSelect }: ISpotTableProps) => (
  <DataTable
    columns={getColumns(search)}
    sorting={sorting}
    rows={spots}
    getRowKey={({ id }) => id}
    isLoading={isLoading}
    emptyText={emptyText}
    selection={selection}
    onRowClick={onSelect}
  />
);
