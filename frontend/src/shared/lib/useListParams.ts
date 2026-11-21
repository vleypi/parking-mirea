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

  return {
    search,
    sortValue: sort ? toSortValue(sort, order) : "",
    setSearch: (value: string) => replaceSearchParams({ [LIST_PARAMS.search]: value.trim() || undefined, [LIST_PARAMS.page]: undefined }),
    setSortValue,
    getFilter,
    setFilter: ({ param }: IListFilter, value: string) => replaceSearchParams({ [param]: value || undefined, [LIST_PARAMS.page]: undefined }),
    sort,
    order,
    page,
    pageSize,
    request: { search: search || undefined, sort, order: sort ? order : undefined, page, pageSize },
    sorting: { field: sort, order, onToggle: toggleSort },
    getEmptyText: (emptyListText: string, filters: readonly IListFilter[] = []) =>
      search || filters.some((filter) => getFilter(filter)) ? LIST_TEXT.notFound : emptyListText,
    setPage: (value: number) => replaceSearchParams({ [LIST_PARAMS.page]: value > FIRST_PAGE ? String(value) : undefined }),
    setSort,
  };
};
