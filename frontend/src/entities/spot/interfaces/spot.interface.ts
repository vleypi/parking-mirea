import type { IListRequest } from "@/shared/api";
import type { SpotAvailability, SpotBulkAction, SpotSortField, SpotStatus } from "@/entities/spot/constants/spot.constant";

export interface ISpotTypeSummary {
  id: number;
  code: string;
  title: string;
  active: boolean;
}

export interface ISpot {
  id: string;
  spotNumber: number;
  spotType: ISpotTypeSummary;
  hourlyRate: number;
  zone?: string;
  description?: string;
  status: { code: SpotStatus; title: string };
  covered: boolean;
  availability: { code: SpotAvailability; title: string };
  archived: boolean;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  version: number;
}

export type ISpotListRequest = IListRequest<SpotSortField> & Record<string, string | number | undefined>;

export interface ISpotPayload {
  spotNumber: number;
  spotTypeId: number;
  hourlyRate: number;
  zone: string | null;
  description: string | null;
  status: SpotStatus;
  covered: boolean;
  tags: string[];
}

export interface ISpotUpdatePayload extends ISpotPayload {
  version: number;
}

export interface ISpotBulkPayload {
  ids: string[];
  action: SpotBulkAction;
  status?: SpotStatus;
  tag?: string;
}

export interface ISpotRequestItem {
  id: string;
  code: string;
  ownerName: string;
  licensePlate: string;
  statusCode: string;
  statusTitle: string;
  startTime: string;
  endTime: string;
  cost: number;
  archived: boolean;
}

export interface ISpotRequests {
  requests: ISpotRequestItem[];
  total: number;
}
