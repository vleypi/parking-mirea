"use client";

import { QUERY_KEYS } from "@/shared/api";
import { ROLES } from "@/shared/config";
import { useEntityModal, useListParams, useRowSelection } from "@/shared/lib";
import { BulkActionBar, FormError, ListToolbar, Modal, PageHeader, Pagination } from "@/shared/ui";
import { useReferences } from "@/entities/reference";
import {
  buildSpotListRequest,
  buildSpotTypeFilter,
  type ISpot,
  SPOT_FIELDS,
  SPOT_FILTERS,
  SPOT_LIST_PARAMS,
  SPOT_SORT_FIELDS,
  SPOT_SORT_OPTIONS,
  SPOT_TEXT,
  SPOTS_PATH,
  SpotTable,
  useSpotPage,
} from "@/entities/spot";
import { hasRole, useViewer } from "@/entities/viewer";
import { BulkSpotActions } from "@/features/bulk-spots";
import { CsvTransferActions } from "@/features/csv-transfer";
import { SpotForm } from "@/features/save-spot";
import { SpotCard } from "@/widgets/spot-card";
import { SPOTS_PAGE_TEXT } from "@/views/spots/constants/spots-page.constant";

const SPOT_SORT_FIELD_LIST = Object.values(SPOT_SORT_FIELDS);
const SPOT_LIST_FIELDS = Object.values(SPOT_FIELDS);

export const SpotsCatalog = () => {
  const list = useListParams(SPOT_SORT_FIELD_LIST);
  const { data, isPending, error } = useSpotPage(buildSpotListRequest(list.request, list.getParam));
  const { data: references } = useReferences();
  const { data: viewer } = useViewer();
  const modal = useEntityModal<ISpot>();
  const selection = useRowSelection();
  const canEdit = hasRole(viewer, ROLES.operator);
  const filters = [buildSpotTypeFilter(references?.spotTypes), ...Object.values(SPOT_FILTERS)];
  const numbers = new Map(data?.content.map(({ id, spotNumber }) => [id, `${SPOT_TEXT.spot} ${spotNumber}`]));
  const exportParams = buildSpotListRequest({ ...list.request, page: undefined, size: undefined }, list.getParam);

  return (
    <>
      <PageHeader
        title={SPOTS_PAGE_TEXT.title}
        description={SPOTS_PAGE_TEXT.description}
        actions={
          <CsvTransferActions
            basePath={SPOTS_PATH}
            invalidateKey={QUERY_KEYS.spots}
            exportParams={exportParams}
            fileName={SPOTS_PAGE_TEXT.csvFileName}
            canImport={canEdit}
          />
        }
        add={canEdit ? { label: SPOTS_PAGE_TEXT.add, onClick: modal.openCreate } : undefined}
      />
      <ListToolbar
        list={list}
        placeholder={SPOT_TEXT.searchPlaceholder}
        total={data?.totalElements}
        filters={filters}
        fields={SPOT_LIST_FIELDS}
        sortOptions={SPOT_SORT_OPTIONS}
      />
      <FormError message={error?.message} />
      {canEdit && (
        <BulkActionBar count={selection.ids.length} onClear={selection.clear}>
          <BulkSpotActions ids={selection.ids} getLabel={(id) => numbers.get(id) ?? id} onDone={selection.clear} />
        </BulkActionBar>
      )}
      <SpotTable
        spots={data?.content}
        isLoading={isPending}
        search={list.search}
        emptyText={list.getEmptyText(SPOT_TEXT.empty, SPOT_LIST_PARAMS)}
        sorting={list.sorting}
        selection={canEdit ? selection : undefined}
        onSelect={modal.openEdit}
      />
      <Pagination page={list.page} pageSize={list.pageSize} total={data?.totalElements ?? 0} onChange={list.setPage} />
      <Modal
        size="xl"
        isOpen={modal.isOpen && modal.entity !== null}
        title={modal.entity ? `${SPOTS_PAGE_TEXT.spotTitle} ${modal.entity.spotNumber}` : ""}
        onClose={modal.close}
      >
        {modal.entity && (
          <SpotCard spotId={modal.entity.id} canEdit={canEdit} canRemovePermanently={hasRole(viewer, ROLES.admin)} onRemoved={modal.close} />
        )}
      </Modal>
      <Modal size="lg" isOpen={modal.isOpen && modal.entity === null} title={SPOTS_PAGE_TEXT.createTitle} onClose={modal.close}>
        <SpotForm spot={null} onDone={modal.close} />
      </Modal>
    </>
  );
};
