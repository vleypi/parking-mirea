"use client";

import { useId, useState } from "react";
import { joinClassNames, useDebouncedDraft } from "@/shared/lib";
import { CONTROL_CLASS_NAME, LABEL_CLASS_NAME } from "@/shared/ui";
import { OWNER_PICKER_TEXT, OWNER_SEARCH_LIMIT } from "@/entities/owner/constants/owner.constant";
import { useOwnerPage } from "@/entities/owner/hooks/useOwnerPage";
import type { IOwnerOption } from "@/entities/owner/interfaces/owner.interface";

interface IOwnerPickerProps {
  selected: IOwnerOption | null;
  error?: string;
  onSelect: (owner: IOwnerOption) => void;
}

/** Выбор владельца поиском по имени или телефону (активные владельцы), вместо ввода UUID. */
export const OwnerPicker = ({ selected, error, onSelect }: IOwnerPickerProps) => {
  const id = useId();
  const [isSearching, setIsSearching] = useState(selected === null);
  const [query, setQuery] = useState("");
  const { draft, setDraft } = useDebouncedDraft(query, setQuery);
  const { data, isFetching } = useOwnerPage({ q: query || undefined, size: OWNER_SEARCH_LIMIT, sort: "name", direction: "asc" });

  const choose = (owner: IOwnerOption) => {
    onSelect(owner);
    setIsSearching(false);
  };

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className={LABEL_CLASS_NAME}>
        {OWNER_PICKER_TEXT.owner}
      </label>
      {!isSearching && selected ? (
        <div className="flex items-center justify-between gap-2 rounded-lg bg-canvas px-3 py-2 text-sm">
          <span>
            <span className="font-medium text-ink">{selected.name}</span> <span className="text-gray-500">{selected.phone}</span>
          </span>
          <button type="button" className="text-sm font-medium text-brand hover:underline" onClick={() => setIsSearching(true)}>
            {OWNER_PICKER_TEXT.change}
          </button>
        </div>
      ) : (
        <>
          <input
            id={id}
            value={draft}
            placeholder={OWNER_PICKER_TEXT.search}
            aria-invalid={Boolean(error)}
            onChange={(event) => setDraft(event.target.value)}
            className={joinClassNames(CONTROL_CLASS_NAME, error ? "border-red-300" : "border-gray-200")}
          />
          <ul className={joinClassNames("flex max-h-48 flex-col overflow-y-auto rounded-lg border border-gray-100", isFetching && "opacity-60")}>
            {data?.content.length === 0 && <li className="px-3 py-2 text-sm text-gray-500">{OWNER_PICKER_TEXT.notFound}</li>}
            {data?.content.map((owner) => (
              <li key={owner.id}>
                <button
                  type="button"
                  className="flex w-full justify-between gap-2 px-3 py-2 text-left text-sm hover:bg-gray-50"
                  onClick={() => choose({ id: owner.id, name: owner.name, phone: owner.phone, archived: owner.archived })}
                >
                  <span className="text-ink">{owner.name}</span>
                  <span className="text-gray-500 tabular-nums">{owner.phone}</span>
                </button>
              </li>
            ))}
          </ul>
        </>
      )}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  );
};
