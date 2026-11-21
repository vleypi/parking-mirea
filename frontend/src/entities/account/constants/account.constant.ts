import { ROLE_LABELS, ROLES, SORT_ORDERS } from "@/shared/config";
import { toSortValue } from "@/shared/lib";

export const ACCOUNT_SORT_FIELDS = {
  login: "login",
  displayName: "displayName",
  createdAt: "createdAt",
} as const;

export type AccountSortField = (typeof ACCOUNT_SORT_FIELDS)[keyof typeof ACCOUNT_SORT_FIELDS];

export const ACCOUNT_ROLE_OPTIONS = Object.values(ROLES).map((role) => ({ value: role, label: ROLE_LABELS[role] }));

export const ACCOUNT_SORT_OPTIONS = [
  { value: "", label: "Сначала новые" },
  { value: toSortValue(ACCOUNT_SORT_FIELDS.createdAt, SORT_ORDERS.asc), label: "Сначала старые" },
  { value: toSortValue(ACCOUNT_SORT_FIELDS.displayName, SORT_ORDERS.asc), label: "По имени, А–Я" },
  { value: toSortValue(ACCOUNT_SORT_FIELDS.login, SORT_ORDERS.asc), label: "По логину" },
];

export const ACCOUNT_TEXT = {
  account: "Сотрудник",
  role: "Роль",
  status: "Статус",
  createdAt: "Создан",
  active: "Активен",
  disabled: "Отключён",
  locked: "Вход заблокирован",
  mustChangePassword: "Временный пароль",
  empty: "Пользователей пока нет",
  searchPlaceholder: "Имя или логин",
  temporaryPassword: "Временный пароль",
  temporaryPasswordHint: "Пароль показывается один раз. Передайте его сотруднику: при первом входе он задаст свой",
  copy: "Скопировать",
  copied: "Скопировано",
} as const;
