"use client";

import { useSearchParams } from "next/navigation";
import { DEFAULT_PAGE_SIZE, FIRST_PAGE, LIST_PARAMS, LIST_TEXT, SORT_ORDERS, SORT_VALUE_SEPARATOR, type SortOrder } from "@/shared/config";
import type { IListFilter } from "@/shared/lib/list.interface";
import { replaceSearchParams } from "@/shared/lib/search-params.util";
import { toSortValue } from "@/shared/lib/sort-value.util";

const parsePage = (value: string | null): number => {
  const page = Number(value);
  return Number.isInteger(page) && page > FIRST_PAGE ? page : FIRST_PAGE;
};

export const useListParams = <TSort extends string>(sortFields: readonly TSort[], pageSize: number = DEFAULT_PAGE_SIZE) => {
  const searchParams = useSearchParams();
  const sortParam = searchParams.get(LIST_PARAMS.sort);
  const sort = sortFields.find((field) => field === sortParam);
  const order: SortOrder = searchParams.get(LIST_PARAMS.order) === SORT_ORDERS.desc ? SORT_ORDERS.desc : SORT_ORDERS.asc;
  const search = searchParams.get(LIST_PARAMS.search) ?? "";
  const page = parsePage(searchParams.get(LIST_PARAMS.page));

  const getParam = (param: string): string => searchParams.get(param) ?? "";
  const setParam = (param: string, value: string) => replaceSearchParams({ [param]: value || undefined, [LIST_PARAMS.page]: undefined });

  const setSort = (field: TSort | undefined, nextOrder: SortOrder = SORT_ORDERS.asc) =>
    replaceSearchParams({
      [LIST_PARAMS.sort]: field,
      [LIST_PARAMS.order]: field && nextOrder === SORT_ORDERS.desc ? nextOrder : undefined,
      [LIST_PARAMS.page]: undefined,
    });

  const toggleSort = (field: TSort) => setSort(field, sort === field && order === SORT_ORDERS.asc ? SORT_ORDERS.desc : SORT_ORDERS.asc);

  const setSortValue = (value: string) => {
    const [field, nextOrder] = value.split(SORT_VALUE_SEPARATOR);
    setSort(
      sortFields.find((sortField) => sortField === field),
      nextOrder === SORT_ORDERS.desc ? SORT_ORDERS.desc : SORT_ORDERS.asc,
    );
  };

  const getFilter = <TValue extends string>({ param, options }: IListFilter<TValue>): TValue | undefined =>
    options.find(({ value }) => value === searchParams.get(param))?.value;

  const resetParams = (params: readonly string[]) =>
    replaceSearchParams(Object.fromEntries([LIST_PARAMS.search, LIST_PARAMS.page, ...params].map((param) => [param, undefined])));

  const hasAnyParam = (params: readonly string[]) => Boolean(search) || params.some((param) => searchParams.has(param));

  return {
    search,
    sortValue: sort ? toSortValue(sort, order) : "",
    setSearch: (value: string) => setParam(LIST_PARAMS.search, value.trim()),
    setSortValue,
    getFilter,
    setFilter: ({ param }: IListFilter, value: string) => setParam(param, value),
    getParam,
    setParam,
    resetParams,
    hasAnyParam,
    sort,
    order,
    page,
    pageSize,
    request: { q: search || undefined, sort, direction: sort ? order : undefined, page: page - FIRST_PAGE, size: pageSize },
    sorting: { field: sort, order, onToggle: toggleSort },
    getEmptyText: (emptyListText: string, params: readonly string[] = []) => (hasAnyParam(params) ? LIST_TEXT.notFound : emptyListText),
    setPage: (value: number) => replaceSearchParams({ [LIST_PARAMS.page]: value > FIRST_PAGE ? String(value) : undefined }),
    setSort,
  };
};
