"use client";

import { QUERY_KEYS } from "@/shared/api";
import { ROLES } from "@/shared/config";
import { useEntityModal, useListParams, useRowSelection } from "@/shared/lib";
import { BulkActionBar, FormError, ListToolbar, Modal, PageHeader, Pagination } from "@/shared/ui";
import { useOwnerPage } from "@/entities/owner";
import {
  buildOwnerFilter,
  buildVehicleListRequest,
  type IVehicle,
  VEHICLE_FIELDS,
  VEHICLE_FILTERS,
  VEHICLE_LIST_PARAMS,
  VEHICLE_SORT_FIELDS,
  VEHICLE_SORT_OPTIONS,
  VEHICLE_TEXT,
  VEHICLES_PATH,
  VehicleTable,
  useVehiclePage,
} from "@/entities/vehicle";
import { hasRole, useViewer } from "@/entities/viewer";
import { BulkVehicleActions } from "@/features/bulk-vehicles";
import { CsvTransferActions } from "@/features/csv-transfer";
import { VehicleForm } from "@/features/save-vehicle";
import { VehicleCard } from "@/widgets/vehicle-card";
import { OWNER_FILTER_REQUEST, VEHICLES_PAGE_TEXT } from "@/views/vehicles/constants/vehicles-page.constant";

const VEHICLE_SORT_FIELD_LIST = Object.values(VEHICLE_SORT_FIELDS);
const VEHICLE_LIST_FIELDS = Object.values(VEHICLE_FIELDS);

export const VehiclesCatalog = () => {
  const list = useListParams(VEHICLE_SORT_FIELD_LIST);
  const request = buildVehicleListRequest(list.request, list.getParam);
  const { data, isPending, error } = useVehiclePage(request);
  const { data: owners } = useOwnerPage(OWNER_FILTER_REQUEST);
  const { data: viewer } = useViewer();
  const modal = useEntityModal<IVehicle>();
  const selection = useRowSelection();
  const canEdit = hasRole(viewer, ROLES.operator);
  const filters = [buildOwnerFilter(owners?.content), ...Object.values(VEHICLE_FILTERS)];
  const plates = new Map(data?.content.map(({ id, licensePlate }) => [id, licensePlate]));
  const exportParams = buildVehicleListRequest({ ...list.request, page: undefined, size: undefined }, list.getParam);

  return (
    <>
      <PageHeader
        title={VEHICLES_PAGE_TEXT.title}
        description={VEHICLES_PAGE_TEXT.description}
        actions={
          <CsvTransferActions
            basePath={VEHICLES_PATH}
            invalidateKey={QUERY_KEYS.vehicles}
            exportParams={exportParams}
            fileName={VEHICLES_PAGE_TEXT.csvFileName}
            canImport={canEdit}
          />
        }
        add={canEdit ? { label: VEHICLES_PAGE_TEXT.add, onClick: modal.openCreate } : undefined}
      />
      <ListToolbar
        list={list}
        placeholder={VEHICLE_TEXT.searchPlaceholder}
        total={data?.totalElements}
        filters={filters}
        fields={VEHICLE_LIST_FIELDS}
        sortOptions={VEHICLE_SORT_OPTIONS}
      />
      <FormError message={error?.message} />
      {canEdit && (
        <BulkActionBar count={selection.ids.length} onClear={selection.clear}>
          <BulkVehicleActions ids={selection.ids} getLabel={(id) => plates.get(id) ?? id} onDone={selection.clear} />
        </BulkActionBar>
      )}
      <VehicleTable
        vehicles={data?.content}
        isLoading={isPending}
        search={list.search}
        emptyText={list.getEmptyText(VEHICLE_TEXT.empty, VEHICLE_LIST_PARAMS)}
        sorting={list.sorting}
        selection={canEdit ? selection : undefined}
        onSelect={modal.openEdit}
      />
      <Pagination page={list.page} pageSize={list.pageSize} total={data?.totalElements ?? 0} onChange={list.setPage} />
      <Modal size="xl" isOpen={modal.isOpen && modal.entity !== null} title={modal.entity?.licensePlate ?? ""} onClose={modal.close}>
        {modal.entity && (
          <VehicleCard
            vehicleId={modal.entity.id}
            canEdit={canEdit}
            canRemovePermanently={hasRole(viewer, ROLES.admin)}
            onRemoved={modal.close}
          />
        )}
      </Modal>
      <Modal size="lg" isOpen={modal.isOpen && modal.entity === null} title={VEHICLES_PAGE_TEXT.createTitle} onClose={modal.close}>
        <VehicleForm vehicle={null} onDone={modal.close} />
      </Modal>
    </>
  );
};
