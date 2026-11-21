"use client";

import { useListParams } from "@/shared/lib";
import { FormError, ListToolbar, PageHeader, Pagination } from "@/shared/ui";
import {
  buildRequestListRequest,
  REQUEST_FIELDS,
  REQUEST_FILTERS,
  REQUEST_LIST_PARAMS,
  REQUEST_SORT_FIELDS,
  REQUEST_SORT_OPTIONS,
  REQUEST_TEXT,
  RequestTable,
  useRequestPage,
} from "@/entities/request";
import { REQUESTS_PAGE_TEXT } from "@/views/requests/constants/requests-page.constant";

const REQUEST_SORT_FIELD_LIST = Object.values(REQUEST_SORT_FIELDS);
const REQUEST_LIST_FILTERS = Object.values(REQUEST_FILTERS);
const REQUEST_LIST_FIELDS = Object.values(REQUEST_FIELDS);

export const RequestsCatalog = () => {
  const list = useListParams(REQUEST_SORT_FIELD_LIST);
  const { data, isPending, error } = useRequestPage(buildRequestListRequest(list.request, list.getParam));

  return (
    <>
      <PageHeader title={REQUESTS_PAGE_TEXT.title} description={REQUESTS_PAGE_TEXT.description} />
      <ListToolbar
        list={list}
        placeholder={REQUEST_TEXT.searchPlaceholder}
        total={data?.totalElements}
        filters={REQUEST_LIST_FILTERS}
        fields={REQUEST_LIST_FIELDS}
        sortOptions={REQUEST_SORT_OPTIONS}
      />
      <FormError message={error?.message} />
      <RequestTable
        requests={data?.content}
        isLoading={isPending}
        search={list.search}
        emptyText={list.getEmptyText(REQUEST_TEXT.empty, REQUEST_LIST_PARAMS)}
        sorting={list.sorting}
      />
      <Pagination page={list.page} pageSize={list.pageSize} total={data?.totalElements ?? 0} onChange={list.setPage} />
    </>
  );
};
