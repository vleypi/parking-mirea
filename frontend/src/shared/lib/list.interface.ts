import type { ISelectOption } from "@/shared/ui";

export interface IListFilterOption<TValue extends string = string> extends ISelectOption {
  value: TValue;
}

export interface IListFilter<TValue extends string = string> {
  param: string;
  allLabel: string;
  options: readonly IListFilterOption<TValue>[];
}

export interface IListControls {
  search: string;
  sortValue: string;
  setSearch: (value: string) => void;
  setSortValue: (value: string) => void;
  getFilter: <TValue extends string>(filter: IListFilter<TValue>) => TValue | undefined;
  setFilter: (filter: IListFilter, value: string) => void;
}
