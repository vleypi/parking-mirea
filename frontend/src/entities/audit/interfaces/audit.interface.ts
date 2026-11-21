import type { IListRequest } from "@/shared/api";
import type { AuditEntityType, AuditEventType, AuditSortField } from "@/entities/audit/constants/audit.constant";

export interface ICodeTitle<TCode extends string> {
  code: TCode;
  title: string;
}

export interface IAuditEvent {
  id: string;
  occurredAt: string;
  actor?: { id?: string; login?: string };
  eventType: ICodeTitle<AuditEventType>;
  entityType?: ICodeTitle<AuditEntityType>;
  entityId?: string;
  summary: string;
  correlationId?: string;
}

export interface IAuditListRequest extends IListRequest<AuditSortField> {
  eventType?: string;
  entityType?: string;
  from?: string;
  to?: string;
}
