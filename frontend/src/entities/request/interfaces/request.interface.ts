import type { IListRequest } from "@/shared/api";
import type { RequestBulkAction, RequestPriority, RequestSortField, RequestStatus, SpotType } from "@/entities/request/constants/request.constant";

export interface IRequestOwner {
  id: string;
  name: string;
  phone: string;
}

export interface IRequestVehicle {
  id: string;
  licensePlate: string;
  brand: string;
  model: string;
}

export interface IRequestSpot {
  id: string;
  number: number;
  type: SpotType;
  typeTitle: string;
  hourlyRate: number;
}

export interface IRequestStatus {
  code: RequestStatus;
  title: string;
}

export interface IRequest {
  id: string;
  code: string;
  owner: IRequestOwner;
  vehicle: IRequestVehicle;
  spot: IRequestSpot;
  status: IRequestStatus;
  priority: RequestPriority;
  startTime: string;
  endTime: string;
  description?: string;
  cost: number;
  paid: boolean;
  archived: boolean;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  version: number;
}

export type IRequestListRequest = IListRequest<RequestSortField> & Record<string, string | number | undefined>;

export interface IRequestUpdatePayload {
  vehicleId: string;
  spotId: string;
  startTime: string;
  endTime: string;
  status: RequestStatus;
  description: string;
  paid: boolean;
  tags: string[];
  version: number;
}

export interface IRequestBulkPayload {
  ids: string[];
  action: RequestBulkAction;
  status?: RequestStatus;
  tag?: string;
}
