"use client";

import { useState, type ReactNode } from "react";
import type { ISelectOption } from "@/shared/ui/SelectControl";
import { LIST_TEXT } from "@/shared/config";
import { joinClassNames } from "@/shared/lib";
import type { IListControls, IListField, IListFilter } from "@/shared/lib/list.interface";
import { Button } from "@/shared/ui/Button";
import { Icon } from "@/shared/ui/Icon";
import { Modal } from "@/shared/ui/Modal";
import { SearchField } from "@/shared/ui/SearchField";
import { ToolbarField } from "@/shared/ui/ToolbarField";
import { ToolbarMenu } from "@/shared/ui/ToolbarMenu";
import { UI_TEXT } from "@/shared/ui/ui.constant";

interface IListToolbarProps {
  list: IListControls;
  placeholder: string;
  total: number | undefined;
  filters?: readonly IListFilter[];
  fields?: readonly IListField[];
  sortOptions?: readonly ISelectOption[];
  extraFilters?: ReactNode;
}

export const ListToolbar = ({ list, placeholder, total, filters = [], fields = [], sortOptions, extraFilters }: IListToolbarProps) => {
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const filterParams = [...filters, ...fields].map(({ param }) => param);
  const hasActiveFilters = filterParams.some((param) => list.getParam(param) !== "");
  const isFiltered = Boolean(list.search) || hasActiveFilters;
  const hasAnyFilter = filters.length > 0 || fields.length > 0 || Boolean(extraFilters);

  const resetFilters = () => list.resetParams(filterParams);

  return (
    <>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <SearchField value={list.search} placeholder={placeholder} onSearch={list.setSearch} />
          {hasAnyFilter && (
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                onClick={() => setIsFilterOpen(true)}
                className={joinClassNames(
                  "inline-flex h-8 shrink-0 items-center gap-2 rounded-full border px-3.5 text-sm font-medium transition",
                  hasActiveFilters
                    ? "border-brand bg-brand text-white hover:bg-brand/90"
                    : "border-gray-300 bg-white text-ink hover:border-gray-400 hover:bg-gray-50",
                )}
              >
                <Icon name="filter" className="size-3.5" />
                {LIST_TEXT.filters}
              </button>
              {hasActiveFilters && (
                <button
                  type="button"
                  onClick={resetFilters}
                  aria-label={LIST_TEXT.resetFilters}
                  title={LIST_TEXT.resetFilters}
                  className="grid size-8 shrink-0 place-items-center rounded-full border border-brand/30 bg-brand-soft text-brand transition hover:bg-brand/15"
                >
                  <Icon name="close" className="size-3.5" />
                </button>
              )}
            </div>
          )}
          {isFiltered && total !== undefined && (
            <p className="px-1 text-sm text-gray-500 tabular-nums">
              {LIST_TEXT.found}: {total}
            </p>
          )}
        </div>
        {sortOptions && (
          <ToolbarMenu label={LIST_TEXT.sort} options={sortOptions} value={list.sortValue} align="end" onChange={list.setSortValue} />
        )}
      </div>

      <Modal size="md" isOpen={isFilterOpen} title={LIST_TEXT.filters} onClose={() => setIsFilterOpen(false)}>
        <div className="flex flex-col gap-5 pb-6">
          {filters.map((filter) => {
            const value = list.getFilter(filter) ?? "";
            return (
              <div key={filter.param} className="flex flex-col gap-2">
                <span className="text-xs font-medium text-gray-500">{filter.allLabel}</span>
                <div className="flex flex-wrap gap-1.5">
                  {[{ value: "", label: filter.allLabel }, ...filter.options].map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => list.setFilter(filter, option.value)}
                      className={joinClassNames(
                        "rounded-full border px-3 py-1 text-sm transition",
                        value === option.value
                          ? "border-brand bg-brand font-medium text-white"
                          : "border-gray-200 bg-white text-gray-700 hover:border-gray-300 hover:bg-gray-50",
                      )}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </div>
            );
          })}
          {fields.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {fields.map((field) => (
                <ToolbarField key={field.param} field={field} value={list.getParam(field.param)} onChange={(value) => list.setParam(field.param, value)} />
              ))}
            </div>
          )}
          {extraFilters && <div className="border-t border-gray-100 pt-4">{extraFilters}</div>}
          <div className="flex items-center justify-between border-t border-gray-100 pt-4">
            {hasActiveFilters ? (
              <Button variant="ghost" onClick={resetFilters}>
                {LIST_TEXT.resetFilters}
              </Button>
            ) : (
              <span />
            )}
            <Button onClick={() => setIsFilterOpen(false)}>{UI_TEXT.close}</Button>
          </div>
        </div>
      </Modal>
    </>
  );
};
