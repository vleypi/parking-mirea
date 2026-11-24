import type { IListRequest } from "@/shared/api";
import type { IListFilter } from "@/shared/lib";
import { SPOT_FIELDS, SPOT_FILTERS, SPOT_TEXT, SPOT_TYPE_PARAM, type SpotSortField } from "@/entities/spot/constants/spot.constant";
import type { ISpotListRequest } from "@/entities/spot/interfaces/spot.interface";

export const SPOT_LIST_PARAMS = [SPOT_TYPE_PARAM, ...[...Object.values(SPOT_FILTERS), ...Object.values(SPOT_FIELDS)].map(({ param }) => param)];

/** В фильтре есть и отключённые типы: по ним могли остаться места. */
export const buildSpotTypeFilter = (types: readonly { id: number; title: string }[] = []): IListFilter => ({
  param: SPOT_TYPE_PARAM,
  allLabel: SPOT_TEXT.allTypes,
  options: types.map(({ id, title }) => ({ value: String(id), label: title })),
});

/** Параметры списка из адреса страницы; операция тарифа без самого тарифа не отправляется. */
export const buildSpotListRequest = (base: IListRequest<SpotSortField>, getParam: (param: string) => string): ISpotListRequest => {
  const filters: Record<string, string | undefined> = Object.fromEntries(SPOT_LIST_PARAMS.map((param) => [param, getParam(param) || undefined]));
  if (!filters[SPOT_FIELDS.rate.param]) filters[SPOT_FILTERS.rateOp.param] = undefined;

  return { ...base, ...filters };
};
