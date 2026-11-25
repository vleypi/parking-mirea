import { BOOLEAN_FILTER_VALUES, type BooleanFilterValue, SORT_ORDERS } from "@/shared/config";
import { type IListField, type IListFilter, toSortValue } from "@/shared/lib";
import type { BadgeTone } from "@/shared/ui";

export const REQUEST_SORT_FIELDS = {
  code: "code",
  startTime: "startTime",
  cost: "cost",
  createdAt: "createdAt",
} as const;

export type RequestSortField = (typeof REQUEST_SORT_FIELDS)[keyof typeof REQUEST_SORT_FIELDS];

export const REQUEST_STATUSES = {
  new: "NEW",
  confirmed: "CONFIRMED",
  completed: "COMPLETED",
  cancelled: "CANCELLED",
} as const;

export type RequestStatus = (typeof REQUEST_STATUSES)[keyof typeof REQUEST_STATUSES];

export const REQUEST_STATUS_LABELS: Record<RequestStatus, string> = {
  NEW: "Новая",
  CONFIRMED: "Подтверждена",
  COMPLETED: "Завершена",
  CANCELLED: "Отменена",
};

export const REQUEST_STATUS_TONES: Record<RequestStatus, BadgeTone> = {
  NEW: "brand",
  CONFIRMED: "success",
  COMPLETED: "neutral",
  CANCELLED: "danger",
};

export const ACTIVE_REQUEST_STATUSES: readonly RequestStatus[] = [REQUEST_STATUSES.new, REQUEST_STATUSES.confirmed];

export const REQUEST_STATUS_TRANSITIONS: Record<RequestStatus, readonly RequestStatus[]> = {
  NEW: [REQUEST_STATUSES.new, REQUEST_STATUSES.confirmed, REQUEST_STATUSES.cancelled],
  CONFIRMED: [REQUEST_STATUSES.confirmed, REQUEST_STATUSES.completed, REQUEST_STATUSES.cancelled],
  COMPLETED: [REQUEST_STATUSES.completed],
  CANCELLED: [REQUEST_STATUSES.cancelled],
};

export const REQUEST_BULK_ACTIONS = {
  archive: "ARCHIVE",
  restore: "RESTORE",
  changeStatus: "CHANGE_STATUS",
  addTag: "ADD_TAG",
  removeTag: "REMOVE_TAG",
} as const;

export type RequestBulkAction = (typeof REQUEST_BULK_ACTIONS)[keyof typeof REQUEST_BULK_ACTIONS];

export const REQUEST_PRIORITIES = {
  high: "HIGH",
  medium: "MEDIUM",
  low: "LOW",
} as const;

export type RequestPriority = (typeof REQUEST_PRIORITIES)[keyof typeof REQUEST_PRIORITIES];

export const REQUEST_PRIORITY_LABELS: Record<RequestPriority, string> = {
  HIGH: "Высокий",
  MEDIUM: "Средний",
  LOW: "Низкий",
};

export const REQUEST_PRIORITY_TONES: Record<RequestPriority, BadgeTone> = {
  HIGH: "danger",
  MEDIUM: "warning",
  LOW: "neutral",
};

export const SPOT_TYPES = {
  standard: "STANDARD",
  disabled: "DISABLED",
  electric: "ELECTRIC",
} as const;

export type SpotType = (typeof SPOT_TYPES)[keyof typeof SPOT_TYPES];

export const SPOT_TYPE_LABELS: Record<SpotType, string> = {
  STANDARD: "Стандартное",
  DISABLED: "Для людей с инвалидностью",
  ELECTRIC: "Для электромобилей",
};

const COST_OPERATIONS = {
  greater: "gt",
  greaterOrEqual: "gte",
  less: "lt",
  lessOrEqual: "lte",
} as const;

type CostOperation = (typeof COST_OPERATIONS)[keyof typeof COST_OPERATIONS];

const ARCHIVE_SCOPES = {
  archived: "ARCHIVED",
  all: "ALL",
} as const;

type ArchiveScope = (typeof ARCHIVE_SCOPES)[keyof typeof ARCHIVE_SCOPES];

export const REQUEST_FILTERS = {
  status: {
    param: "status",
    allLabel: "Любой статус",
    options: Object.values(REQUEST_STATUSES).map((status) => ({ value: status, label: REQUEST_STATUS_LABELS[status] })),
  } satisfies IListFilter<RequestStatus>,
  spotType: {
    param: "spotType",
    allLabel: "Любое место",
    options: Object.values(SPOT_TYPES).map((type) => ({ value: type, label: SPOT_TYPE_LABELS[type] })),
  } satisfies IListFilter<SpotType>,
  paid: {
    param: "paid",
    allLabel: "Оплата: все",
    options: [
      { value: BOOLEAN_FILTER_VALUES.yes, label: "Оплачены" },
      { value: BOOLEAN_FILTER_VALUES.no, label: "Не оплачены" },
    ],
  } satisfies IListFilter<BooleanFilterValue>,
  priority: {
    param: "priority",
    allLabel: "Любой приоритет",
    options: Object.values(REQUEST_PRIORITIES).map((priority) => ({ value: priority, label: REQUEST_PRIORITY_LABELS[priority] })),
  } satisfies IListFilter<RequestPriority>,
  archived: {
    param: "archived",
    allLabel: "Активные",
    options: [
      { value: ARCHIVE_SCOPES.archived, label: "Архив" },
      { value: ARCHIVE_SCOPES.all, label: "Активные и архив" },
    ],
  } satisfies IListFilter<ArchiveScope>,
  costOp: {
    param: "costOp",
    allLabel: "Стоимость =",
    options: [
      { value: COST_OPERATIONS.greater, label: "Стоимость >" },
      { value: COST_OPERATIONS.greaterOrEqual, label: "Стоимость ≥" },
      { value: COST_OPERATIONS.less, label: "Стоимость <" },
      { value: COST_OPERATIONS.lessOrEqual, label: "Стоимость ≤" },
    ],
  } satisfies IListFilter<CostOperation>,
};

export const REQUEST_FIELDS = {
  cost: { param: "cost", label: "₽", type: "number" },
  tag: { param: "tag", label: "Тег", type: "text" },
  from: { param: "from", label: "Начало с", type: "datetime-local" },
  to: { param: "to", label: "по", type: "datetime-local" },
} satisfies Record<string, IListField>;

export const REQUEST_SORT_OPTIONS = [
  { value: "", label: "Сначала новые" },
  { value: toSortValue(REQUEST_SORT_FIELDS.createdAt, SORT_ORDERS.asc), label: "Сначала старые" },
  { value: toSortValue(REQUEST_SORT_FIELDS.startTime, SORT_ORDERS.asc), label: "По началу, раньше" },
  { value: toSortValue(REQUEST_SORT_FIELDS.startTime, SORT_ORDERS.desc), label: "По началу, позже" },
  { value: toSortValue(REQUEST_SORT_FIELDS.cost, SORT_ORDERS.desc), label: "Сначала дорогие" },
  { value: toSortValue(REQUEST_SORT_FIELDS.cost, SORT_ORDERS.asc), label: "Сначала дешёвые" },
  { value: toSortValue(REQUEST_SORT_FIELDS.code, SORT_ORDERS.asc), label: "По номеру" },
];

export const REQUEST_TEXT = {
  code: "Номер",
  owner: "Владелец",
  vehicle: "Автомобиль",
  spot: "Место",
  period: "Период",
  cost: "Стоимость",
  status: "Статус",
  priority: "Приоритет",
  paid: "Оплачена",
  unpaid: "Не оплачена",
  archived: "В архиве",
  spotNumber: "№",
  tags: "Теги",
  description: "Описание",
  createdAt: "Создана",
  updatedAt: "Изменена",
  hourlyRate: "/ч",
  empty: "Заявок пока нет",
  searchPlaceholder: "Номер, госномер или владелец",
} as const;
