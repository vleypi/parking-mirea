import type { SortOrder } from "@/shared/config";

export interface IPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface IListRequest<TSort extends string> {
  q?: string;
  sort?: TSort;
  direction?: SortOrder;
  page?: number;
  size?: number;
}
