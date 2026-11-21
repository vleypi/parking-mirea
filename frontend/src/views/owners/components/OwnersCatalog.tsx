"use client";

import { ROLES } from "@/shared/config";
import { useEntityModal, useListParams, useRowSelection } from "@/shared/lib";
import { BulkActionBar, FormError, ListToolbar, Modal, PageHeader, Pagination } from "@/shared/ui";
import {
  buildCategoryFilter,
  buildOwnerListRequest,
  type IOwner,
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
import { hasRole, useViewer } from "@/entities/viewer";
import { BulkOwnerActions } from "@/features/bulk-owners";
import { OwnerForm } from "@/features/save-owner";
import { OwnerCard } from "@/widgets/owner-card";
import { OWNERS_PAGE_TEXT } from "@/views/owners/constants/owners-page.constant";

const OWNER_SORT_FIELD_LIST = Object.values(OWNER_SORT_FIELDS);
const OWNER_LIST_FIELDS = Object.values(OWNER_FIELDS);

export const OwnersCatalog = () => {
  const list = useListParams(OWNER_SORT_FIELD_LIST);
  const { data, isPending, error } = useOwnerPage(buildOwnerListRequest(list.request, list.getParam));
  const { data: categories } = useClientCategories();
  const { data: viewer } = useViewer();
  const modal = useEntityModal<IOwner>();
  const selection = useRowSelection();
  const canEdit = hasRole(viewer, ROLES.operator);
  const filters = [buildCategoryFilter(categories), ...Object.values(OWNER_FILTERS)];
  const names = new Map(data?.content.map(({ id, name }) => [id, name]));

  return (
    <>
      <PageHeader
        title={OWNERS_PAGE_TEXT.title}
        description={OWNERS_PAGE_TEXT.description}
        add={canEdit ? { label: OWNERS_PAGE_TEXT.add, onClick: modal.openCreate } : undefined}
      />
      <ListToolbar
        list={list}
        placeholder={OWNER_TEXT.searchPlaceholder}
        total={data?.totalElements}
        filters={filters}
        fields={OWNER_LIST_FIELDS}
        sortOptions={OWNER_SORT_OPTIONS}
      />
      <FormError message={error?.message} />
      {canEdit && (
        <BulkActionBar count={selection.ids.length} onClear={selection.clear}>
          <BulkOwnerActions ids={selection.ids} getLabel={(id) => names.get(id) ?? id} onDone={selection.clear} />
        </BulkActionBar>
      )}
      <OwnerTable
        owners={data?.content}
        isLoading={isPending}
        search={list.search}
        emptyText={list.getEmptyText(OWNER_TEXT.empty, OWNER_LIST_PARAMS)}
        sorting={list.sorting}
        selection={canEdit ? selection : undefined}
        onSelect={modal.openEdit}
      />
      <Pagination page={list.page} pageSize={list.pageSize} total={data?.totalElements ?? 0} onChange={list.setPage} />
      <Modal size="xl" isOpen={modal.isOpen && modal.entity !== null} title={modal.entity?.name ?? ""} onClose={modal.close}>
        {modal.entity && (
          <OwnerCard
            ownerId={modal.entity.id}
            canEdit={canEdit}
            canRemovePermanently={hasRole(viewer, ROLES.admin)}
            onRemoved={modal.close}
          />
        )}
      </Modal>
      <Modal isOpen={modal.isOpen && modal.entity === null} title={OWNERS_PAGE_TEXT.createTitle} onClose={modal.close}>
        <OwnerForm owner={null} onDone={modal.close} />
      </Modal>
    </>
  );
};
