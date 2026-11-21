"use client";

import { Icon } from "@/shared/ui/Icon";
import { joinClassNames } from "@/shared/lib/join-class-names";
import { useDebouncedDraft } from "@/shared/lib/useDebouncedDraft";
import { LIST_TEXT } from "@/shared/config";
import { TOOLBAR_CONTROL_CLASS_NAME } from "@/shared/ui/ui.constant";

interface ISearchFieldProps {
  value: string;
  placeholder: string;
  onSearch: (value: string) => void;
}

export const SearchField = ({ value, placeholder, onSearch }: ISearchFieldProps) => {
  const { draft, setDraft, clear } = useDebouncedDraft(value, onSearch);

  return (
    <div className="relative w-full sm:w-80">
      <Icon name="search" className="pointer-events-none absolute top-1/2 left-3 size-3.5 -translate-y-1/2 text-ink" />
      <input
        type="search"
        value={draft}
        placeholder={placeholder}
        aria-label={placeholder}
        onChange={(event) => setDraft(event.target.value)}
        className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-full pr-8 pl-8.5 [&::-webkit-search-cancel-button]:hidden")}
      />
      {draft && (
        <button
          type="button"
          onClick={clear}
          aria-label={LIST_TEXT.clearSearch}
          className="absolute top-1/2 right-1.5 grid size-5 -translate-y-1/2 place-items-center rounded-md text-gray-400 transition hover:bg-gray-100 hover:text-ink"
        >
          <Icon name="close" className="size-3" />
        </button>
      )}
    </div>
  );
};
