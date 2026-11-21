import type { IListRequest } from "@/shared/api";
import type { IListFilter } from "@/shared/lib";
import { OWNER_CATEGORY_PARAM, OWNER_FIELDS, OWNER_FILTERS, OWNER_TEXT, type OwnerSortField } from "@/entities/owner/constants/owner.constant";
import type { IClientCategory, IOwnerListRequest } from "@/entities/owner/interfaces/owner.interface";

export const OWNER_LIST_PARAMS = [OWNER_CATEGORY_PARAM, ...[...Object.values(OWNER_FILTERS), ...Object.values(OWNER_FIELDS)].map(({ param }) => param)];

export const buildCategoryFilter = (categories: readonly IClientCategory[] = []): IListFilter => ({
  param: OWNER_CATEGORY_PARAM,
  allLabel: OWNER_TEXT.allCategories,
  options: categories.map(({ id, title }) => ({ value: String(id), label: title })),
});

export const buildOwnerListRequest = (base: IListRequest<OwnerSortField>, getParam: (param: string) => string): IOwnerListRequest => {
  const filters: Record<string, string | undefined> = Object.fromEntries(OWNER_LIST_PARAMS.map((param) => [param, getParam(param) || undefined]));
  if (!filters[OWNER_FIELDS.discount.param]) filters[OWNER_FILTERS.discountOp.param] = undefined;

  return { ...base, ...filters };
};
