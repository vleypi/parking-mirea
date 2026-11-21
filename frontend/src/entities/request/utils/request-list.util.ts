import type { IListRequest } from "@/shared/api";
import { REQUEST_FIELDS, REQUEST_FILTERS, type RequestSortField } from "@/entities/request/constants/request.constant";
import type { IRequestListRequest } from "@/entities/request/interfaces/request.interface";

export const REQUEST_LIST_PARAMS = [...Object.values(REQUEST_FILTERS), ...Object.values(REQUEST_FIELDS)].map(({ param }) => param);

export const buildRequestListRequest = (base: IListRequest<RequestSortField>, getParam: (param: string) => string): IRequestListRequest => {
  const filters: Record<string, string | undefined> = Object.fromEntries(REQUEST_LIST_PARAMS.map((param) => [param, getParam(param) || undefined]));
  if (!filters[REQUEST_FIELDS.cost.param]) filters[REQUEST_FILTERS.costOp.param] = undefined;

  return { ...base, ...filters };
};
