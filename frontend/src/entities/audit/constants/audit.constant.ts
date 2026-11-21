import { SORT_ORDERS } from "@/shared/config";
import { type IListField, type IListFilter, toSortValue } from "@/shared/lib";
import type { BadgeTone } from "@/shared/ui";

export const AUDIT_SORT_FIELDS = {
  occurredAt: "occurredAt",
  eventType: "eventType",
  actorLogin: "actorLogin",
} as const;

export type AuditSortField = (typeof AUDIT_SORT_FIELDS)[keyof typeof AUDIT_SORT_FIELDS];

export const AUDIT_EVENT_TYPES = {
  loginSuccess: "LOGIN_SUCCESS",
  loginFailure: "LOGIN_FAILURE",
  create: "CREATE",
  update: "UPDATE",
  archive: "ARCHIVE",
  restore: "RESTORE",
  delete: "DELETE",
} as const;

export type AuditEventType = (typeof AUDIT_EVENT_TYPES)[keyof typeof AUDIT_EVENT_TYPES];

export const AUDIT_EVENT_TYPE_LABELS: Record<AuditEventType, string> = {
  LOGIN_SUCCESS: "Успешный вход",
  LOGIN_FAILURE: "Неуспешный вход",
  CREATE: "Создание",
  UPDATE: "Изменение",
  ARCHIVE: "Архивирование",
  RESTORE: "Восстановление",
  DELETE: "Окончательное удаление",
};

export const AUDIT_EVENT_TYPE_TONES: Record<AuditEventType, BadgeTone> = {
  LOGIN_SUCCESS: "success",
  LOGIN_FAILURE: "danger",
  CREATE: "brand",
  UPDATE: "brand",
  ARCHIVE: "warning",
  RESTORE: "neutral",
  DELETE: "danger",
};

export const AUDIT_ENTITY_TYPES = {
  request: "REQUEST",
  owner: "OWNER",
  vehicle: "VEHICLE",
  spot: "SPOT",
  userAccount: "USER_ACCOUNT",
  reference: "REFERENCE",
} as const;

export type AuditEntityType = (typeof AUDIT_ENTITY_TYPES)[keyof typeof AUDIT_ENTITY_TYPES];

export const AUDIT_ENTITY_TYPE_LABELS: Record<AuditEntityType, string> = {
  REQUEST: "Заявка",
  OWNER: "Владелец",
  VEHICLE: "Автомобиль",
  SPOT: "Парковочное место",
  USER_ACCOUNT: "Учётная запись",
  REFERENCE: "Справочник",
};

export const AUDIT_FILTERS = {
  eventType: {
    param: "eventType",
    allLabel: "Любое событие",
    options: Object.values(AUDIT_EVENT_TYPES).map((type) => ({ value: type, label: AUDIT_EVENT_TYPE_LABELS[type] })),
  } satisfies IListFilter<AuditEventType>,
  entityType: {
    param: "entityType",
    allLabel: "Любая сущность",
    options: Object.values(AUDIT_ENTITY_TYPES).map((type) => ({ value: type, label: AUDIT_ENTITY_TYPE_LABELS[type] })),
  } satisfies IListFilter<AuditEntityType>,
};

export const AUDIT_FIELDS = {
  from: { param: "from", label: "С", type: "datetime-local" },
  to: { param: "to", label: "по", type: "datetime-local" },
} satisfies Record<string, IListField>;

export const AUDIT_SORT_OPTIONS = [
  { value: "", label: "Сначала новые" },
  { value: toSortValue(AUDIT_SORT_FIELDS.occurredAt, SORT_ORDERS.asc), label: "Сначала старые" },
  { value: toSortValue(AUDIT_SORT_FIELDS.eventType, SORT_ORDERS.asc), label: "По типу события" },
  { value: toSortValue(AUDIT_SORT_FIELDS.actorLogin, SORT_ORDERS.asc), label: "По пользователю" },
];

export const AUDIT_TEXT = {
  occurredAt: "Время",
  event: "Событие",
  actor: "Пользователь",
  entity: "Объект",
  summary: "Описание",
  correlationId: "Correlation ID",
  system: "Система",
  empty: "Событий пока нет",
  searchPlaceholder: "Логин, описание или correlation id",
} as const;
