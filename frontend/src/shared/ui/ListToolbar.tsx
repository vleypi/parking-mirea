import type { ISelectOption } from "@/shared/ui/SelectControl";
import { LIST_TEXT } from "@/shared/config";
import type { IListControls, IListFilter } from "@/shared/lib";
import { SearchField } from "@/shared/ui/SearchField";
import { ToolbarMenu } from "@/shared/ui/ToolbarMenu";

interface IListToolbarProps {
  list: IListControls;
  placeholder: string;
  total: number | undefined;
  filters?: readonly IListFilter[];
  sortOptions?: readonly ISelectOption[];
}

export const ListToolbar = ({ list, placeholder, total, filters = [], sortOptions }: IListToolbarProps) => (
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
    {list.search && total !== undefined && (
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
