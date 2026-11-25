import type { IListRequest } from "@/shared/api";
import type { VehicleActivity, VehicleBulkAction, VehicleSortField, VehicleStatus } from "@/entities/vehicle/constants/vehicle.constant";

export interface IVehicleOwner {
  id: string;
  name: string;
  phone: string;
  archived: boolean;
}

export interface IVehicle {
  id: string;
  licensePlate: string;
  region: string;
  brand: string;
  model: string;
  color?: string;
  releaseYear?: number;
  electric: boolean;
  status: { code: VehicleStatus; title: string };
  owner: IVehicleOwner;
  recentRequests: number;
  activity: { code: VehicleActivity; title: string };
  archived: boolean;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  version: number;
}

export type IVehicleListRequest = IListRequest<VehicleSortField> & Record<string, string | number | undefined>;

export interface IVehiclePayload {
  ownerId: string;
  licensePlate: string;
  brand: string;
  model: string;
  color: string | null;
  releaseYear: number | null;
  electric: boolean;
  status: VehicleStatus;
  tags: string[];
}

export interface IVehicleUpdatePayload extends IVehiclePayload {
  version: number;
}

export interface IVehicleBulkPayload {
  ids: string[];
  action: VehicleBulkAction;
  status?: VehicleStatus;
  tag?: string;
}

export interface IVehicleRequestItem {
  id: string;
  code: string;
  ownerName: string;
  spotNumber: number;
  statusCode: string;
  statusTitle: string;
  startTime: string;
  endTime: string;
  cost: number;
  archived: boolean;
}

export interface IVehicleRequests {
  requests: IVehicleRequestItem[];
  total: number;
}
