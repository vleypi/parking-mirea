import type { ISelectOption } from "@/shared/ui";

export interface IListFilterOption<TValue extends string = string> extends ISelectOption {
  value: TValue;
}

export interface IListFilter<TValue extends string = string> {
  param: string;
  allLabel: string;
  options: readonly IListFilterOption<TValue>[];
}

export type ListFieldType = "text" | "number" | "datetime-local";

export interface IListField {
  param: string;
  label: string;
  type: ListFieldType;
}

export interface IListControls {
  search: string;
  sortValue: string;
  setSearch: (value: string) => void;
  setSortValue: (value: string) => void;
  getFilter: <TValue extends string>(filter: IListFilter<TValue>) => TValue | undefined;
  setFilter: (filter: IListFilter, value: string) => void;
  getParam: (param: string) => string;
  setParam: (param: string, value: string) => void;
  resetParams: (params: readonly string[]) => void;
}

export interface IRowSelection {
  ids: string[];
  isSelected: (key: string) => boolean;
  toggle: (key: string) => void;
  toggleAll: (keys: readonly string[]) => void;
  clear: () => void;
}
