import { BOOLEAN_FILTER_VALUES, type BooleanFilterValue, SORT_ORDERS } from "@/shared/config";
import { type IListField, type IListFilter, toSortValue } from "@/shared/lib";
import type { BadgeTone } from "@/shared/ui";

export const SPOT_SORT_FIELDS = {
  spotNumber: "spotNumber",
  hourlyRate: "hourlyRate",
  zone: "zone",
  createdAt: "createdAt",
} as const;

export type SpotSortField = (typeof SPOT_SORT_FIELDS)[keyof typeof SPOT_SORT_FIELDS];

export const SPOT_STATUSES = {
  available: "AVAILABLE",
  maintenance: "MAINTENANCE",
  closed: "CLOSED",
} as const;

export type SpotStatus = (typeof SPOT_STATUSES)[keyof typeof SPOT_STATUSES];

export const SPOT_STATUS_LABELS: Record<SpotStatus, string> = {
  AVAILABLE: "Работает",
  MAINTENANCE: "На обслуживании",
  CLOSED: "Закрыто",
};

export const SPOT_STATUS_TONES: Record<SpotStatus, BadgeTone> = {
  AVAILABLE: "success",
  MAINTENANCE: "warning",
  CLOSED: "neutral",
};

/** Доступность сейчас: считает сервер по статусу места и текущей заявке. */
export const SPOT_AVAILABILITIES = {
  free: "FREE_NOW",
  busy: "BUSY_NOW",
  unavailable: "UNAVAILABLE",
} as const;

export type SpotAvailability = (typeof SPOT_AVAILABILITIES)[keyof typeof SPOT_AVAILABILITIES];

export const SPOT_AVAILABILITY_LABELS: Record<SpotAvailability, string> = {
  FREE_NOW: "Свободно",
  BUSY_NOW: "Занято",
  UNAVAILABLE: "Недоступно",
};

export const SPOT_AVAILABILITY_TONES: Record<SpotAvailability, BadgeTone> = {
  FREE_NOW: "success",
  BUSY_NOW: "danger",
  UNAVAILABLE: "neutral",
};

export const SPOT_BULK_ACTIONS = {
  archive: "ARCHIVE",
  restore: "RESTORE",
  changeStatus: "CHANGE_STATUS",
  addTag: "ADD_TAG",
  removeTag: "REMOVE_TAG",
} as const;

export type SpotBulkAction = (typeof SPOT_BULK_ACTIONS)[keyof typeof SPOT_BULK_ACTIONS];

const RATE_OPERATIONS = {
  greater: "gt",
  greaterOrEqual: "gte",
  less: "lt",
  lessOrEqual: "lte",
} as const;

type RateOperation = (typeof RATE_OPERATIONS)[keyof typeof RATE_OPERATIONS];

const ARCHIVE_SCOPES = {
  archived: "ARCHIVED",
  all: "ALL",
} as const;

type ArchiveScope = (typeof ARCHIVE_SCOPES)[keyof typeof ARCHIVE_SCOPES];

/** Параметр фильтра по типу: варианты приходят из справочника, см. buildSpotTypeFilter. */
export const SPOT_TYPE_PARAM = "spotTypeId";

export const MAX_HOURLY_RATE = 99999.99;

export const SPOT_FILTERS = {
  status: {
    param: "status",
    allLabel: "Любой статус",
    options: Object.values(SPOT_STATUSES).map((status) => ({ value: status, label: SPOT_STATUS_LABELS[status] })),
  } satisfies IListFilter<SpotStatus>,
  covered: {
    param: "covered",
    allLabel: "Крытые и открытые",
    options: [
      { value: BOOLEAN_FILTER_VALUES.yes, label: "Крытые" },
      { value: BOOLEAN_FILTER_VALUES.no, label: "Открытые" },
    ],
  } satisfies IListFilter<BooleanFilterValue>,
  availability: {
    param: "availability",
    allLabel: "Любая доступность",
    options: Object.values(SPOT_AVAILABILITIES).map((availability) => ({ value: availability, label: SPOT_AVAILABILITY_LABELS[availability] })),
  } satisfies IListFilter<SpotAvailability>,
  archived: {
    param: "archived",
    allLabel: "Активные",
    options: [
      { value: ARCHIVE_SCOPES.archived, label: "Архив" },
      { value: ARCHIVE_SCOPES.all, label: "Активные и архив" },
    ],
  } satisfies IListFilter<ArchiveScope>,
  rateOp: {
    param: "rateOp",
    allLabel: "Тариф =",
    options: [
      { value: RATE_OPERATIONS.greater, label: "Тариф >" },
      { value: RATE_OPERATIONS.greaterOrEqual, label: "Тариф ≥" },
      { value: RATE_OPERATIONS.less, label: "Тариф <" },
      { value: RATE_OPERATIONS.lessOrEqual, label: "Тариф ≤" },
    ],
  } satisfies IListFilter<RateOperation>,
};

export const SPOT_FIELDS = {
  rate: { param: "rate", label: "Тариф, ₽/ч", type: "number" },
  tag: { param: "tag", label: "Тег", type: "text" },
  freeFrom: { param: "freeFrom", label: "Свободно с", type: "datetime-local" },
  freeTo: { param: "freeTo", label: "по", type: "datetime-local" },
} satisfies Record<string, IListField>;

export const SPOT_SORT_OPTIONS = [
  { value: "", label: "По номеру" },
  { value: toSortValue(SPOT_SORT_FIELDS.spotNumber, SORT_ORDERS.desc), label: "По номеру, с конца" },
  { value: toSortValue(SPOT_SORT_FIELDS.hourlyRate, SORT_ORDERS.asc), label: "Сначала дешевле" },
  { value: toSortValue(SPOT_SORT_FIELDS.hourlyRate, SORT_ORDERS.desc), label: "Сначала дороже" },
  { value: toSortValue(SPOT_SORT_FIELDS.zone, SORT_ORDERS.asc), label: "По зоне" },
  { value: toSortValue(SPOT_SORT_FIELDS.createdAt, SORT_ORDERS.desc), label: "Сначала новые" },
];

export const SPOT_TEXT = {
  spotNumber: "Номер",
  spot: "Место",
  type: "Тип",
  typeDisabled: "тип отключён",
  allTypes: "Любой тип",
  rate: "Тариф",
  perHour: "в час",
  perHourShort: "/ч",
  zone: "Зона",
  description: "Описание",
  status: "Статус",
  availability: "Сейчас",
  covered: "Крытое",
  yes: "Да",
  no: "Нет",
  archived: "В архиве",
  tags: "Теги",
  noTags: "Тегов нет",
  notSet: "Не указано",
  createdAt: "Добавлено",
  updatedAt: "Изменено",
  requestsTitle: "Заявки на место",
  requestsDescription: "Сначала новые, вместе с архивными",
  requestsShown: "Показаны последние",
  requestsOf: "из",
  noRequests: "Заявок нет",
  loadRequestsFailed: "Не удалось загрузить заявки",
  empty: "Мест пока нет",
  searchPlaceholder: "Номер, зона или описание",
} as const;
