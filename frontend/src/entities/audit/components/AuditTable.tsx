"use client";

import { formatDateTime } from "@/shared/lib";
import { Badge, DataTable, Highlight, type IDataTableColumn, type IDataTableSorting, TableTitleCell } from "@/shared/ui";
import { AUDIT_EVENT_TYPE_TONES, AUDIT_SORT_FIELDS, AUDIT_TEXT, type AuditSortField } from "@/entities/audit/constants/audit.constant";
import type { IAuditEvent } from "@/entities/audit/interfaces/audit.interface";

interface IAuditTableProps {
  events: IAuditEvent[] | undefined;
  isLoading: boolean;
  search: string;
  emptyText: string;
  sorting: IDataTableSorting<AuditSortField>;
}

const getColumns = (search: string): IDataTableColumn<IAuditEvent, AuditSortField>[] => [
  {
    key: "occurredAt",
    header: AUDIT_TEXT.occurredAt,
    sortField: AUDIT_SORT_FIELDS.occurredAt,
    render: ({ occurredAt }) => <span className="whitespace-nowrap tabular-nums">{formatDateTime(occurredAt)}</span>,
  },
  {
    key: "event",
    header: AUDIT_TEXT.event,
    sortField: AUDIT_SORT_FIELDS.eventType,
    render: ({ eventType }) => <Badge tone={AUDIT_EVENT_TYPE_TONES[eventType.code]}>{eventType.title}</Badge>,
  },
  {
    key: "actor",
    header: AUDIT_TEXT.actor,
    sortField: AUDIT_SORT_FIELDS.actorLogin,
    render: ({ actor }) => <Highlight text={actor?.login ?? AUDIT_TEXT.system} query={search} />,
  },
  {
    key: "summary",
    header: AUDIT_TEXT.summary,
    render: ({ summary, entityType, correlationId }) => (
      <TableTitleCell
        title={<Highlight text={summary} query={search} />}
        subtitle={
          <>
            {entityType && <span>{entityType.title} · </span>}
            <span className="font-mono text-xs">
              {AUDIT_TEXT.correlationId}: <Highlight text={correlationId ?? "—"} query={search} />
            </span>
          </>
        }
      />
    ),
  },
];

export const AuditTable = ({ events, isLoading, search, emptyText, sorting }: IAuditTableProps) => (
  <DataTable columns={getColumns(search)} sorting={sorting} rows={events} getRowKey={({ id }) => id} isLoading={isLoading} emptyText={emptyText} />
);
