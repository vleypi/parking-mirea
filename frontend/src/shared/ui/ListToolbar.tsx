import type { ISelectOption } from "@/shared/ui/SelectControl";
import { LIST_TEXT } from "@/shared/config";
import type { IListControls, IListField, IListFilter } from "@/shared/lib/list.interface";
import { Button } from "@/shared/ui/Button";
import { SearchField } from "@/shared/ui/SearchField";
import { ToolbarField } from "@/shared/ui/ToolbarField";
import { ToolbarMenu } from "@/shared/ui/ToolbarMenu";

interface IListToolbarProps {
  list: IListControls;
  placeholder: string;
  total: number | undefined;
  filters?: readonly IListFilter[];
  fields?: readonly IListField[];
  sortOptions?: readonly ISelectOption[];
}

export const ListToolbar = ({ list, placeholder, total, filters = [], fields = [], sortOptions }: IListToolbarProps) => {
  const params = [...filters, ...fields].map(({ param }) => param);
  const isFiltered = Boolean(list.search) || params.some((param) => list.getParam(param) !== "");

  return (
    <div className="mb-6 flex flex-wrap items-center gap-2">
      <SearchField value={list.search} placeholder={placeholder} onSearch={list.setSearch} />
      {filters.map((filter) => {
        const value = list.getFilter(filter) ?? "";

        return (
          <ToolbarMenu
            key={filter.param}
            label={filter.allLabel}
            options={[{ value: "", label: filter.allLabel }, ...filter.options]}
            value={value}
            isApplied={value !== ""}
            onChange={(nextValue) => list.setFilter(filter, nextValue)}
          />
        );
      })}
      {fields.map((field) => (
        <ToolbarField key={field.param} field={field} value={list.getParam(field.param)} onChange={(value) => list.setParam(field.param, value)} />
      ))}
      {isFiltered && (
        <Button variant="ghost" onClick={() => list.resetParams(params)} className="h-8 px-3">
          {LIST_TEXT.resetFilters}
        </Button>
      )}
      {isFiltered && total !== undefined && (
        <p className="px-1 text-sm text-gray-500 tabular-nums">
          {LIST_TEXT.found}: {total}
        </p>
      )}
      {sortOptions && (
        <div className="ml-auto">
          <ToolbarMenu label={LIST_TEXT.sort} options={sortOptions} value={list.sortValue} align="end" onChange={list.setSortValue} />
        </div>
      )}
    </div>
  );
};
