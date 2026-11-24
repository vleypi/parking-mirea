import { SORT_ORDERS } from "@/shared/config";
import { OWNER_SORT_FIELDS, type IOwnerListRequest } from "@/entities/owner";

export const VEHICLES_PAGE_TEXT = {
  title: "Автомобили",
  description: "Машины клиентов: поиск, фильтры и сортировка. Фильтр сохраняется в адресе страницы",
  add: "Добавить автомобиль",
  createTitle: "Новый автомобиль",
  csvFileName: "vehicles.csv",
} as const;

/** Владельцы для фильтра: первые 100 по имени. */
export const OWNER_FILTER_REQUEST: IOwnerListRequest = { page: 0, size: 100, sort: OWNER_SORT_FIELDS.name, direction: SORT_ORDERS.asc };
