import { BOOLEAN_FILTER_VALUES, type BooleanFilterValue, SORT_ORDERS } from "@/shared/config";
import { type IListField, type IListFilter, toSortValue } from "@/shared/lib";
import type { BadgeTone } from "@/shared/ui";

export const VEHICLE_SORT_FIELDS = {
  licensePlate: "licensePlate",
  brand: "brand",
  releaseYear: "releaseYear",
  createdAt: "createdAt",
} as const;

export type VehicleSortField = (typeof VEHICLE_SORT_FIELDS)[keyof typeof VEHICLE_SORT_FIELDS];

export const VEHICLE_STATUSES = {
  active: "ACTIVE",
  blocked: "BLOCKED",
  deregistered: "DEREGISTERED",
} as const;

export type VehicleStatus = (typeof VEHICLE_STATUSES)[keyof typeof VEHICLE_STATUSES];

export const VEHICLE_STATUS_LABELS: Record<VehicleStatus, string> = {
  ACTIVE: "Допущен",
  BLOCKED: "Въезд запрещён",
  DEREGISTERED: "Снят с учёта",
};

export const VEHICLE_STATUS_TONES: Record<VehicleStatus, BadgeTone> = {
  ACTIVE: "success",
  BLOCKED: "danger",
  DEREGISTERED: "neutral",
};

/** Активность: заявки без отменённых за 30 дней — 0 LOW, 1–3 MEDIUM, 4+ HIGH. Считает сервер. */
export const VEHICLE_ACTIVITIES = {
  low: "LOW",
  medium: "MEDIUM",
  high: "HIGH",
} as const;

export type VehicleActivity = (typeof VEHICLE_ACTIVITIES)[keyof typeof VEHICLE_ACTIVITIES];

export const VEHICLE_ACTIVITY_LABELS: Record<VehicleActivity, string> = {
  LOW: "Низкая",
  MEDIUM: "Средняя",
  HIGH: "Высокая",
};

export const VEHICLE_ACTIVITY_TONES: Record<VehicleActivity, BadgeTone> = {
  LOW: "neutral",
  MEDIUM: "brand",
  HIGH: "warning",
};

export const VEHICLE_BULK_ACTIONS = {
  archive: "ARCHIVE",
  restore: "RESTORE",
  changeStatus: "CHANGE_STATUS",
  addTag: "ADD_TAG",
  removeTag: "REMOVE_TAG",
} as const;

export type VehicleBulkAction = (typeof VEHICLE_BULK_ACTIONS)[keyof typeof VEHICLE_BULK_ACTIONS];

const YEAR_OPERATIONS = {
  greater: "gt",
  greaterOrEqual: "gte",
  less: "lt",
  lessOrEqual: "lte",
} as const;

type YearOperation = (typeof YEAR_OPERATIONS)[keyof typeof YEAR_OPERATIONS];

const ARCHIVE_SCOPES = {
  archived: "ARCHIVED",
  all: "ALL",
} as const;

type ArchiveScope = (typeof ARCHIVE_SCOPES)[keyof typeof ARCHIVE_SCOPES];

/** Параметр фильтра по владельцу: варианты приходят из списка владельцев, см. buildOwnerFilter. */
export const VEHICLE_OWNER_PARAM = "ownerId";

export const LICENSE_PLATE_EXAMPLE = "А123ВС777";
export const LICENSE_PLATE_LETTERS = "АВЕКМНОРСТУХ";
export const MIN_RELEASE_YEAR = 1980;

export const VEHICLE_FILTERS = {
  status: {
    param: "status",
    allLabel: "Любой статус",
    options: Object.values(VEHICLE_STATUSES).map((status) => ({ value: status, label: VEHICLE_STATUS_LABELS[status] })),
  } satisfies IListFilter<VehicleStatus>,
  electric: {
    param: "electric",
    allLabel: "Любой двигатель",
    options: [
      { value: BOOLEAN_FILTER_VALUES.yes, label: "Электромобили" },
      { value: BOOLEAN_FILTER_VALUES.no, label: "Не электромобили" },
    ],
  } satisfies IListFilter<BooleanFilterValue>,
  activity: {
    param: "activity",
    allLabel: "Любая активность",
    options: Object.values(VEHICLE_ACTIVITIES).map((activity) => ({ value: activity, label: VEHICLE_ACTIVITY_LABELS[activity] })),
  } satisfies IListFilter<VehicleActivity>,
  archived: {
    param: "archived",
    allLabel: "Активные",
    options: [
      { value: ARCHIVE_SCOPES.archived, label: "Архив" },
      { value: ARCHIVE_SCOPES.all, label: "Активные и архив" },
    ],
  } satisfies IListFilter<ArchiveScope>,
  yearOp: {
    param: "yearOp",
    allLabel: "Год =",
    options: [
      { value: YEAR_OPERATIONS.greater, label: "Год >" },
      { value: YEAR_OPERATIONS.greaterOrEqual, label: "Год ≥" },
      { value: YEAR_OPERATIONS.less, label: "Год <" },
      { value: YEAR_OPERATIONS.lessOrEqual, label: "Год ≤" },
    ],
  } satisfies IListFilter<YearOperation>,
};

export const VEHICLE_FIELDS = {
  year: { param: "year", label: "Год", type: "number" },
  region: { param: "region", label: "Регион", type: "text" },
  tag: { param: "tag", label: "Тег", type: "text" },
} satisfies Record<string, IListField>;

export const VEHICLE_SORT_OPTIONS = [
  { value: "", label: "Сначала новые" },
  { value: toSortValue(VEHICLE_SORT_FIELDS.createdAt, SORT_ORDERS.asc), label: "Сначала старые" },
  { value: toSortValue(VEHICLE_SORT_FIELDS.licensePlate, SORT_ORDERS.asc), label: "По номеру" },
  { value: toSortValue(VEHICLE_SORT_FIELDS.brand, SORT_ORDERS.asc), label: "По марке, А–Я" },
  { value: toSortValue(VEHICLE_SORT_FIELDS.releaseYear, SORT_ORDERS.desc), label: "Сначала новее" },
  { value: toSortValue(VEHICLE_SORT_FIELDS.releaseYear, SORT_ORDERS.asc), label: "Сначала старше" },
];

export const VEHICLE_TEXT = {
  licensePlate: "Гос. номер",
  region: "Регион",
  vehicle: "Марка и модель",
  owner: "Владелец",
  allOwners: "Любой владелец",
  status: "Статус",
  activity: "Активность",
  activityHint: "Заявки за 30 дней без отменённых",
  year: "Год",
  color: "Цвет",
  electric: "Электромобиль",
  yes: "Да",
  no: "Нет",
  archived: "В архиве",
  ownerArchived: "владелец в архиве",
  tags: "Теги",
  noTags: "Тегов нет",
  notSet: "Не указан",
  createdAt: "Добавлен",
  updatedAt: "Изменён",
  requestsTitle: "Заявки",
  requestsDescription: "Сначала новые, вместе с архивными",
  requestsShown: "Показаны последние",
  requestsOf: "из",
  noRequests: "Заявок нет",
  spot: "Место",
  loadRequestsFailed: "Не удалось загрузить заявки",
  empty: "Автомобилей пока нет",
  searchPlaceholder: "Гос. номер, марка или модель",
} as const;
