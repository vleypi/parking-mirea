import { joinClassNames } from "@/shared/lib/join-class-names";
import type { IRowSelection } from "@/shared/lib/list.interface";
import { Icon } from "@/shared/ui/Icon";
import type { KeyboardEvent, ReactNode } from "react";
import { SORT_ORDERS, type SortOrder } from "@/shared/config";
import { UI_TEXT } from "@/shared/ui/ui.constant";

export interface IDataTableColumn<TRow, TSort extends string = string> {
  key: string;
  header: string;
  className?: string;
  sortField?: TSort;
  render: (row: TRow) => ReactNode;
}

export interface IDataTableSorting<TSort extends string> {
  field: TSort | undefined;
  order: SortOrder;
  onToggle: (field: TSort) => void;
}

interface IDataTableProps<TRow, TSort extends string> {
  columns: readonly IDataTableColumn<TRow, TSort>[];
  sorting?: IDataTableSorting<TSort>;
  rows: readonly TRow[] | undefined;
  getRowKey: (row: TRow) => string;
  isLoading: boolean;
  emptyText: string;
  onRowClick?: (row: TRow) => void;
  isRowClickable?: (row: TRow) => boolean;
  selection?: IRowSelection;
}

const SKELETON_ROW_KEYS = ["first", "second", "third", "fourth"] as const;

const CELL_CLASS_NAME = "px-4 py-4 text-gray-600";
const ROW_DIVIDER_CLASS_NAME = "border-y border-t-transparent border-b-gray-200 first:border-l first:border-l-transparent last:border-r last:border-r-transparent";
const ROW_CARD_CLASS_NAME = [
  "group-hover/row:border-t-gray-200 group-hover/row:bg-white",
  "group-hover/row:first:rounded-l-xl group-hover/row:first:border-l-gray-200",
  "group-hover/row:last:rounded-r-xl group-hover/row:last:border-r-gray-200",
  "group-focus-visible/row:border-t-gray-200 group-focus-visible/row:bg-white",
  "group-focus-visible/row:first:rounded-l-xl group-focus-visible/row:first:border-l-gray-200",
  "group-focus-visible/row:last:rounded-r-xl group-focus-visible/row:last:border-r-gray-200",
].join(" ");
const ABOVE_CARD_ROW_CLASS_NAME = "[&:has(+tr[tabindex]:hover)>td]:border-b-transparent [&:has(+tr[tabindex]:focus-visible)>td]:border-b-transparent";

const ARIA_SORT: Record<SortOrder, "ascending" | "descending"> = {
  [SORT_ORDERS.asc]: "ascending",
  [SORT_ORDERS.desc]: "descending",
};

export const DataTable = <TRow, TSort extends string = string>({
  columns,
  sorting,
  rows,
  getRowKey,
  isLoading,
  emptyText,
  onRowClick,
  isRowClickable = () => true,
  selection,
}: IDataTableProps<TRow, TSort>) => {
  const pageKeys = rows?.map(getRowKey) ?? [];
  const isPageSelected = pageKeys.length > 0 && pageKeys.every((key) => selection?.isSelected(key));

  const activate = (row: TRow) => {
    if (onRowClick && isRowClickable(row)) onRowClick(row);
  };

  const handleKeyDown = (event: KeyboardEvent, row: TRow) => {
    if (event.key === "Enter") activate(row);
  };

  return (
    <div className="-mx-4 overflow-x-auto">
      <table className="w-full min-w-xl border-separate border-spacing-0 text-left text-[15px]">
        <thead className="text-sm text-gray-600">
          <tr>
            {selection && (
              <th scope="col" className="w-10 px-4 pb-3">
                <input
                  type="checkbox"
                  aria-label={UI_TEXT.selectAll}
                  checked={isPageSelected}
                  onChange={() => selection.toggleAll(pageKeys)}
                  className="size-4 accent-brand"
                />
              </th>
            )}
            {columns.map(({ key, header, className, sortField }) => {
              const isSorted = Boolean(sortField) && sorting?.field === sortField;

              return (
                <th
                  key={key}
                  scope="col"
                  aria-sort={isSorted && sorting ? ARIA_SORT[sorting.order] : undefined}
                  className={joinClassNames("px-4 pb-3 font-normal", className)}
                >
                  {sortField && sorting ? (
                    <button
                      type="button"
                      onClick={() => sorting.onToggle(sortField)}
                      className={joinClassNames("inline-flex items-center gap-1 transition hover:text-ink", isSorted && "font-medium text-ink")}
                    >
                      {header}
                      <Icon name={isSorted ? (sorting.order === SORT_ORDERS.desc ? "arrowDown" : "arrowUp") : "arrowsUpDown"} className="size-3.5" />
                    </button>
                  ) : (
                    header
                  )}
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {isLoading &&
            SKELETON_ROW_KEYS.map((key) => (
              <tr key={key} className="animate-pulse">
                {selection && <td className={joinClassNames(CELL_CLASS_NAME, ROW_DIVIDER_CLASS_NAME)} />}
                {columns.map((column) => (
                  <td key={column.key} className={joinClassNames(CELL_CLASS_NAME, ROW_DIVIDER_CLASS_NAME)}>
                    <div className="h-4 w-3/4 rounded bg-gray-200/70" />
                  </td>
                ))}
              </tr>
            ))}
          {!isLoading && !rows?.length && (
            <tr>
              <td colSpan={columns.length + (selection ? 1 : 0)} className="px-4 py-12 text-center text-gray-500">
                {emptyText}
              </td>
            </tr>
          )}
          {!isLoading &&
            rows?.map((row) => {
              const isClickable = Boolean(onRowClick) && isRowClickable(row);

              return (
                <tr
                  key={getRowKey(row)}
                  tabIndex={isClickable ? 0 : undefined}
                  onClick={() => activate(row)}
                  onKeyDown={(event) => handleKeyDown(event, row)}
                  className={joinClassNames("group/row", ABOVE_CARD_ROW_CLASS_NAME, isClickable && "cursor-pointer focus-visible:outline-none")}
                >
                  {selection && (
                    <td className={joinClassNames(CELL_CLASS_NAME, ROW_DIVIDER_CLASS_NAME, isClickable && ROW_CARD_CLASS_NAME)} onClick={(event) => event.stopPropagation()}>
                      <input
                        type="checkbox"
                        aria-label={UI_TEXT.selectRow}
                        checked={selection.isSelected(getRowKey(row))}
                        onChange={() => selection.toggle(getRowKey(row))}
                        className="size-4 accent-brand"
                      />
                    </td>
                  )}
                  {columns.map(({ key, className, render }) => (
                    <td key={key} className={joinClassNames(CELL_CLASS_NAME, ROW_DIVIDER_CLASS_NAME, isClickable && ROW_CARD_CLASS_NAME, className)}>
                      {render(row)}
                    </td>
                  ))}
                </tr>
              );
            })}
        </tbody>
      </table>
    </div>
  );
};
