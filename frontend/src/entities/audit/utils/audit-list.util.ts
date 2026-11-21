import type { IListRequest } from "@/shared/api";
import { AUDIT_FIELDS, AUDIT_FILTERS, type AuditSortField } from "@/entities/audit/constants/audit.constant";
import type { IAuditListRequest } from "@/entities/audit/interfaces/audit.interface";

export const AUDIT_LIST_PARAMS = [...Object.values(AUDIT_FILTERS), ...Object.values(AUDIT_FIELDS)].map(({ param }) => param);

export const buildAuditListRequest = (base: IListRequest<AuditSortField>, getParam: (param: string) => string): IAuditListRequest => ({
  ...base,
  ...Object.fromEntries(AUDIT_LIST_PARAMS.map((param) => [param, getParam(param) || undefined])),
});
