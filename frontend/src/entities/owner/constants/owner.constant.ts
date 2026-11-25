import { BOOLEAN_FILTER_VALUES, type BooleanFilterValue, SORT_ORDERS } from "@/shared/config";
import { type IListField, type IListFilter, toSortValue } from "@/shared/lib";
import type { BadgeTone } from "@/shared/ui";

export const OWNER_SORT_FIELDS = {
  name: "name",
  createdAt: "createdAt",
  discount: "discount",
} as const;

export type OwnerSortField = (typeof OWNER_SORT_FIELDS)[keyof typeof OWNER_SORT_FIELDS];

export const OWNER_STATUSES = {
  active: "ACTIVE",
  blocked: "BLOCKED",
} as const;

export type OwnerStatus = (typeof OWNER_STATUSES)[keyof typeof OWNER_STATUSES];

export const OWNER_STATUS_LABELS: Record<OwnerStatus, string> = {
  ACTIVE: "Активен",
  BLOCKED: "Заблокирован",
};

export const OWNER_STATUS_TONES: Record<OwnerStatus, BadgeTone> = {
  ACTIVE: "success",
  BLOCKED: "danger",
};

export const LOYALTY_LEVELS = {
  new: "NEW",
  regular: "REGULAR",
  vip: "VIP",
} as const;

export type LoyaltyLevel = (typeof LOYALTY_LEVELS)[keyof typeof LOYALTY_LEVELS];

export const LOYALTY_LABELS: Record<LoyaltyLevel, string> = {
  NEW: "Новый",
  REGULAR: "Постоянный",
  VIP: "VIP",
};

export const LOYALTY_TONES: Record<LoyaltyLevel, BadgeTone> = {
  NEW: "neutral",
  REGULAR: "brand",
  VIP: "warning",
};

export const OWNER_BULK_ACTIONS = {
  archive: "ARCHIVE",
  restore: "RESTORE",
  changeStatus: "CHANGE_STATUS",
  addTag: "ADD_TAG",
  removeTag: "REMOVE_TAG",
} as const;

export type OwnerBulkAction = (typeof OWNER_BULK_ACTIONS)[keyof typeof OWNER_BULK_ACTIONS];

const DISCOUNT_OPERATIONS = {
  greater: "gt",
  greaterOrEqual: "gte",
  less: "lt",
  lessOrEqual: "lte",
} as const;

type DiscountOperation = (typeof DISCOUNT_OPERATIONS)[keyof typeof DISCOUNT_OPERATIONS];

const ARCHIVE_SCOPES = {
  archived: "ARCHIVED",
  all: "ALL",
} as const;

type ArchiveScope = (typeof ARCHIVE_SCOPES)[keyof typeof ARCHIVE_SCOPES];

/** Категория «Обычный»: её получает новый владелец, если не выбрана другая. */
export const CLIENT_CATEGORY_DEFAULT_ID = 1;

/** Параметр фильтра по категории: варианты приходят из справочника, см. buildCategoryFilter. */
export const OWNER_CATEGORY_PARAM = "categoryId";

export const OWNER_FILTERS = {
  status: {
    param: "status",
    allLabel: "Любой статус",
    options: Object.values(OWNER_STATUSES).map((status) => ({ value: status, label: OWNER_STATUS_LABELS[status] })),
  } satisfies IListFilter<OwnerStatus>,
  loyalty: {
    param: "loyalty",
    allLabel: "Любая лояльность",
    options: Object.values(LOYALTY_LEVELS).map((level) => ({ value: level, label: LOYALTY_LABELS[level] })),
  } satisfies IListFilter<LoyaltyLevel>,
  notifications: {
    param: "notifications",
    allLabel: "Уведомления: все",
    options: [
      { value: BOOLEAN_FILTER_VALUES.yes, label: "Уведомления включены" },
      { value: BOOLEAN_FILTER_VALUES.no, label: "Уведомления выключены" },
    ],
  } satisfies IListFilter<BooleanFilterValue>,
  archived: {
    param: "archived",
    allLabel: "Активные",
    options: [
      { value: ARCHIVE_SCOPES.archived, label: "Архив" },
      { value: ARCHIVE_SCOPES.all, label: "Активные и архив" },
    ],
  } satisfies IListFilter<ArchiveScope>,
  discountOp: {
    param: "discountOp",
    allLabel: "Скидка =",
    options: [
      { value: DISCOUNT_OPERATIONS.greater, label: "Скидка >" },
      { value: DISCOUNT_OPERATIONS.greaterOrEqual, label: "Скидка ≥" },
      { value: DISCOUNT_OPERATIONS.less, label: "Скидка <" },
      { value: DISCOUNT_OPERATIONS.lessOrEqual, label: "Скидка ≤" },
    ],
  } satisfies IListFilter<DiscountOperation>,
};

export const OWNER_FIELDS = {
  discount: { param: "discount", label: "%", type: "number" },
  tag: { param: "tag", label: "Тег", type: "text" },
  from: { param: "from", label: "Регистрация с", type: "datetime-local" },
  to: { param: "to", label: "по", type: "datetime-local" },
} satisfies Record<string, IListField>;

export const OWNER_SORT_OPTIONS = [
  { value: "", label: "Сначала новые" },
  { value: toSortValue(OWNER_SORT_FIELDS.createdAt, SORT_ORDERS.asc), label: "Сначала старые" },
  { value: toSortValue(OWNER_SORT_FIELDS.name, SORT_ORDERS.asc), label: "По имени, А–Я" },
  { value: toSortValue(OWNER_SORT_FIELDS.name, SORT_ORDERS.desc), label: "По имени, Я–А" },
  { value: toSortValue(OWNER_SORT_FIELDS.discount, SORT_ORDERS.desc), label: "Сначала с большей скидкой" },
  { value: toSortValue(OWNER_SORT_FIELDS.discount, SORT_ORDERS.asc), label: "Сначала с меньшей скидкой" },
];

export const OWNER_TEXT = {
  owner: "Владелец",
  contacts: "Контакты",
  category: "Категория",
  allCategories: "Любая категория",
  status: "Статус",
  loyalty: "Лояльность",
  discount: "Скидка",
  vehicles: "Машин",
  completedRequests: "Завершённых заявок",
  notifications: "Уведомления",
  notificationsOn: "Включены",
  notificationsOff: "Выключены",
  archived: "В архиве",
  tags: "Теги",
  createdAt: "Регистрация",
  updatedAt: "Изменён",
  noEmail: "Email не указан",
  phone: "Телефон",
  email: "Email",
  noTags: "Тегов нет",
  vehiclesTitle: "Автомобили",
  vehiclesDescription: "Зарегистрированы на владельца",
  requestsDescription: "Сначала новые, вместе с архивными",
  noVehicles: "Автомобилей нет",
  requestsTitle: "Заявки",
  noRequests: "Заявок нет",
  requestsShown: "Показаны последние",
  requestsOf: "из",
  spot: "Место",
  empty: "Владельцев пока нет",
  searchPlaceholder: "Имя, телефон или email",
} as const;

export const OWNER_PICKER_TEXT = {
  owner: "Владелец",
  search: "Имя или телефон владельца",
  notFound: "Никого не нашли",
  change: "Сменить",
} as const;

export const OWNER_SEARCH_LIMIT = 8;
