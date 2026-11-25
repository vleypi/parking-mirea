export { VehicleDetails } from "@/entities/vehicle/components/VehicleDetails";
export { VehicleRequestList } from "@/entities/vehicle/components/VehicleRequestList";
export { VehicleTable } from "@/entities/vehicle/components/VehicleTable";
export {
  LICENSE_PLATE_EXAMPLE,
  LICENSE_PLATE_LETTERS,
  MIN_RELEASE_YEAR,
  VEHICLE_ACTIVITIES,
  VEHICLE_ACTIVITY_LABELS,
  VEHICLE_ACTIVITY_TONES,
  VEHICLE_BULK_ACTIONS,
  VEHICLE_FIELDS,
  VEHICLE_FILTERS,
  VEHICLE_OWNER_PARAM,
  VEHICLE_SORT_FIELDS,
  VEHICLE_SORT_OPTIONS,
  VEHICLE_STATUS_LABELS,
  VEHICLE_STATUS_TONES,
  VEHICLE_STATUSES,
  VEHICLE_TEXT,
  type VehicleActivity,
  type VehicleBulkAction,
  type VehicleSortField,
  type VehicleStatus,
} from "@/entities/vehicle/constants/vehicle.constant";
export { useVehicle, useVehiclePage, useVehicleRequests } from "@/entities/vehicle/hooks/useVehicle";
export type {
  IVehicle,
  IVehicleBulkPayload,
  IVehicleListRequest,
  IVehicleOwner,
  IVehiclePayload,
  IVehicleRequestItem,
  IVehicleRequests,
  IVehicleUpdatePayload,
} from "@/entities/vehicle/interfaces/vehicle.interface";
export { VEHICLES_PATH, vehicleService } from "@/entities/vehicle/services/vehicle.service";
export { buildOwnerFilter, buildVehicleListRequest, VEHICLE_LIST_PARAMS } from "@/entities/vehicle/utils/vehicle-list.util";
