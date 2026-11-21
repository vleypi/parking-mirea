import type { IListRequest } from "@/shared/api";
import type { LoyaltyLevel, OwnerBulkAction, OwnerSortField, OwnerStatus } from "@/entities/owner/constants/owner.constant";

export interface IClientCategory {
  id: number;
  code: string;
  title: string;
  active: boolean;
}

export interface IOwner {
  id: string;
  name: string;
  phone: string;
  email?: string;
  category: IClientCategory;
  status: { code: OwnerStatus; title: string };
  discountPercent: number;
  notificationsEnabled: boolean;
  vehiclesCount: number;
  completedRequests: number;
  loyalty: { code: LoyaltyLevel; title: string };
  archived: boolean;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  version: number;
}

export type IOwnerListRequest = IListRequest<OwnerSortField> & Record<string, string | number | undefined>;

export interface IOwnerPayload {
  name: string;
  phone: string;
  email: string | null;
  categoryId: number;
  status: OwnerStatus;
  discountPercent: number;
  notificationsEnabled: boolean;
  tags: string[];
}

export interface IOwnerUpdatePayload extends IOwnerPayload {
  version: number;
}

export interface IOwnerBulkPayload {
  ids: string[];
  action: OwnerBulkAction;
  status?: OwnerStatus;
  tag?: string;
}
