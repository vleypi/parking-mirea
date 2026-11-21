"use client";

import { useListParams } from "@/shared/lib";
import { FormError, ListToolbar, PageHeader, Pagination } from "@/shared/ui";
import {
  buildCategoryFilter,
  buildOwnerListRequest,
  OWNER_FIELDS,
  OWNER_FILTERS,
  OWNER_LIST_PARAMS,
  OWNER_SORT_FIELDS,
  OWNER_SORT_OPTIONS,
  OWNER_TEXT,
  OwnerTable,
  useClientCategories,
  useOwnerPage,
} from "@/entities/owner";
import { OWNERS_PAGE_TEXT } from "@/views/owners/constants/owners-page.constant";

const OWNER_SORT_FIELD_LIST = Object.values(OWNER_SORT_FIELDS);
const OWNER_LIST_FIELDS = Object.values(OWNER_FIELDS);

export const OwnersCatalog = () => {
  const list = useListParams(OWNER_SORT_FIELD_LIST);
  const { data, isPending, error } = useOwnerPage(buildOwnerListRequest(list.request, list.getParam));
  const { data: categories } = useClientCategories();
  const filters = [buildCategoryFilter(categories), ...Object.values(OWNER_FILTERS)];

  return (
    <>
      <PageHeader title={OWNERS_PAGE_TEXT.title} description={OWNERS_PAGE_TEXT.description} />
      <ListToolbar
        list={list}
        placeholder={OWNER_TEXT.searchPlaceholder}
        total={data?.totalElements}
        filters={filters}
        fields={OWNER_LIST_FIELDS}
        sortOptions={OWNER_SORT_OPTIONS}
      />
      <FormError message={error?.message} />
      <OwnerTable
        owners={data?.content}
        isLoading={isPending}
        search={list.search}
        emptyText={list.getEmptyText(OWNER_TEXT.empty, OWNER_LIST_PARAMS)}
        sorting={list.sorting}
        onSelect={() => undefined}
      />
      <Pagination page={list.page} pageSize={list.pageSize} total={data?.totalElements ?? 0} onChange={list.setPage} />
    </>
  );
};
