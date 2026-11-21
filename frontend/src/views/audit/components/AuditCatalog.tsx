"use client";

import { useListParams } from "@/shared/lib";
import { FormError, ListToolbar, PageHeader, Pagination } from "@/shared/ui";
import {
  AUDIT_FIELDS,
  AUDIT_FILTERS,
  AUDIT_LIST_PARAMS,
  AUDIT_SORT_FIELDS,
  AUDIT_SORT_OPTIONS,
  AUDIT_TEXT,
  AuditTable,
  buildAuditListRequest,
  useAuditPage,
} from "@/entities/audit";
import { AUDIT_PAGE_TEXT } from "@/views/audit/constants/audit-page.constant";

const AUDIT_SORT_FIELD_LIST = Object.values(AUDIT_SORT_FIELDS);
const AUDIT_LIST_FILTERS = Object.values(AUDIT_FILTERS);
const AUDIT_LIST_FIELDS = Object.values(AUDIT_FIELDS);

export const AuditCatalog = () => {
  const list = useListParams(AUDIT_SORT_FIELD_LIST);
  const { data, isPending, error } = useAuditPage(buildAuditListRequest(list.request, list.getParam));

  return (
    <>
      <PageHeader title={AUDIT_PAGE_TEXT.title} description={AUDIT_PAGE_TEXT.description} />
      <ListToolbar
        list={list}
        placeholder={AUDIT_TEXT.searchPlaceholder}
        total={data?.totalElements}
        filters={AUDIT_LIST_FILTERS}
        fields={AUDIT_LIST_FIELDS}
        sortOptions={AUDIT_SORT_OPTIONS}
      />
      <FormError message={error?.message} />
      <AuditTable
        events={data?.content}
        isLoading={isPending}
        search={list.search}
        emptyText={list.getEmptyText(AUDIT_TEXT.empty, AUDIT_LIST_PARAMS)}
        sorting={list.sorting}
      />
      <Pagination page={list.page} pageSize={list.pageSize} total={data?.totalElements ?? 0} onChange={list.setPage} />
    </>
  );
};
