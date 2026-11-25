import type { IListRequest } from "@/shared/api";
import type { IListFilter } from "@/shared/lib";
import {
  VEHICLE_FIELDS,
  VEHICLE_FILTERS,
  VEHICLE_OWNER_PARAM,
  VEHICLE_TEXT,
  type VehicleSortField,
} from "@/entities/vehicle/constants/vehicle.constant";
import type { IVehicleListRequest } from "@/entities/vehicle/interfaces/vehicle.interface";

export const VEHICLE_LIST_PARAMS = [
  VEHICLE_OWNER_PARAM,
  ...[...Object.values(VEHICLE_FILTERS), ...Object.values(VEHICLE_FIELDS)].map(({ param }) => param),
];

export const buildOwnerFilter = (owners: readonly { id: string; name: string }[] = []): IListFilter => ({
  param: VEHICLE_OWNER_PARAM,
  allLabel: VEHICLE_TEXT.allOwners,
  options: owners.map(({ id, name }) => ({ value: id, label: name })),
});

/** Параметры списка из адреса страницы; операция года без самого года не отправляется. */
export const buildVehicleListRequest = (base: IListRequest<VehicleSortField>, getParam: (param: string) => string): IVehicleListRequest => {
  const filters: Record<string, string | undefined> = Object.fromEntries(VEHICLE_LIST_PARAMS.map((param) => [param, getParam(param) || undefined]));
  if (!filters[VEHICLE_FIELDS.year.param]) filters[VEHICLE_FILTERS.yearOp.param] = undefined;

  return { ...base, ...filters };
};
