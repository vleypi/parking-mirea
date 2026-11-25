"use client";

import { QUERY_KEYS } from "@/shared/api";
import { ROLES } from "@/shared/config";
import { useEntityModal, useListParams, useRowSelection } from "@/shared/lib";
import { BulkActionBar, FormError, ListToolbar, Modal, PageHeader, Pagination } from "@/shared/ui";
import {
  buildRequestListRequest,
  type IRequest,
  REQUEST_FIELDS,
  REQUEST_FILTERS,
  REQUEST_LIST_PARAMS,
  REQUEST_SORT_FIELDS,
  REQUEST_SORT_OPTIONS,
  REQUEST_TEXT,
  REQUESTS_PATH,
  RequestTable,
  useRequestPage,
} from "@/entities/request";
import { hasRole, useViewer } from "@/entities/viewer";
import { BulkRequestActions } from "@/features/bulk-requests";
import { CsvTransferActions } from "@/features/csv-transfer";
import { CreateRequestForm } from "@/features/edit-request";
import { RequestCard } from "@/widgets/request-card";
import { REQUESTS_PAGE_TEXT } from "@/views/requests/constants/requests-page.constant";

const REQUEST_SORT_FIELD_LIST = Object.values(REQUEST_SORT_FIELDS);
const REQUEST_LIST_FILTERS = Object.values(REQUEST_FILTERS);
const REQUEST_LIST_FIELDS = Object.values(REQUEST_FIELDS);

export const RequestsCatalog = () => {
  const list = useListParams(REQUEST_SORT_FIELD_LIST);
  const { data, isPending, error } = useRequestPage(buildRequestListRequest(list.request, list.getParam));
  const exportParams = buildRequestListRequest({ ...list.request, page: undefined, size: undefined }, list.getParam);
  const { data: viewer } = useViewer();
  const card = useEntityModal<IRequest>();
  const selection = useRowSelection();
  const canEdit = hasRole(viewer, ROLES.operator);
  const codes = new Map(data?.content.map(({ id, code }) => [id, code]));

  return (
    <>
      <PageHeader
        title={REQUESTS_PAGE_TEXT.title}
        description={REQUESTS_PAGE_TEXT.description}
        actions={
          <CsvTransferActions
            basePath={REQUESTS_PATH}
            invalidateKey={QUERY_KEYS.requests}
            exportParams={exportParams}
            fileName={REQUESTS_PAGE_TEXT.csvFileName}
            canImport={canEdit}
          />
        }
        add={canEdit ? { label: REQUESTS_PAGE_TEXT.add, onClick: card.openCreate } : undefined}
      />
      <ListToolbar
        list={list}
        placeholder={REQUEST_TEXT.searchPlaceholder}
        total={data?.totalElements}
        filters={REQUEST_LIST_FILTERS}
        fields={REQUEST_LIST_FIELDS}
        sortOptions={REQUEST_SORT_OPTIONS}
      />
      <FormError message={error?.message} />
      {canEdit && (
        <BulkActionBar count={selection.ids.length} onClear={selection.clear}>
          <BulkRequestActions ids={selection.ids} getLabel={(id) => codes.get(id) ?? id} onDone={selection.clear} />
        </BulkActionBar>
      )}
      <RequestTable
        requests={data?.content}
        isLoading={isPending}
        search={list.search}
        emptyText={list.getEmptyText(REQUEST_TEXT.empty, REQUEST_LIST_PARAMS)}
        sorting={list.sorting}
        selection={canEdit ? selection : undefined}
        onSelect={card.openEdit}
      />
      <Pagination page={list.page} pageSize={list.pageSize} total={data?.totalElements ?? 0} onChange={list.setPage} />
      <Modal size="xl" isOpen={card.isOpen && card.entity !== null} title={card.entity?.code ?? ""} onClose={card.close}>
        {card.entity && (
          <RequestCard
            requestId={card.entity.id}
            canEdit={canEdit}
            canRemovePermanently={hasRole(viewer, ROLES.admin)}
            onRemoved={card.close}
          />
        )}
      </Modal>
      <Modal size="lg" isOpen={card.isOpen && card.entity === null} title={REQUESTS_PAGE_TEXT.createTitle} onClose={card.close}>
        <CreateRequestForm onDone={card.close} />
      </Modal>
    </>
  );
};
