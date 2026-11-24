"use client";

import { useId, useState } from "react";
import { joinClassNames, useDebouncedDraft } from "@/shared/lib";
import { CONTROL_CLASS_NAME, LABEL_CLASS_NAME } from "@/shared/ui";
import { useOwnerPage } from "@/entities/owner";
import type { IVehicleOwner } from "@/entities/vehicle";
import { OWNER_SEARCH_LIMIT, SAVE_VEHICLE_TEXT } from "@/features/save-vehicle/constants/save-vehicle.constant";

interface IOwnerPickerProps {
  selected: IVehicleOwner | null;
  error?: string;
  onSelect: (owner: IVehicleOwner) => void;
}

/** Выбор владельца поиском по имени или телефону (активные владельцы), вместо ввода UUID. */
export const OwnerPicker = ({ selected, error, onSelect }: IOwnerPickerProps) => {
  const id = useId();
  const [isSearching, setIsSearching] = useState(selected === null);
  const [query, setQuery] = useState("");
  const { draft, setDraft } = useDebouncedDraft(query, setQuery);
  const { data, isFetching } = useOwnerPage({ q: query || undefined, size: OWNER_SEARCH_LIMIT, sort: "name", direction: "asc" });

  const choose = (owner: IVehicleOwner) => {
    onSelect(owner);
    setIsSearching(false);
  };

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className={LABEL_CLASS_NAME}>
        {SAVE_VEHICLE_TEXT.owner}
      </label>
      {!isSearching && selected ? (
        <div className="flex items-center justify-between gap-2 rounded-lg bg-canvas px-3 py-2 text-sm">
          <span>
            <span className="font-medium text-ink">{selected.name}</span> <span className="text-gray-500">{selected.phone}</span>
          </span>
          <button type="button" className="text-sm font-medium text-brand hover:underline" onClick={() => setIsSearching(true)}>
            {SAVE_VEHICLE_TEXT.ownerChange}
          </button>
        </div>
      ) : (
        <>
          <input
            id={id}
            value={draft}
            placeholder={SAVE_VEHICLE_TEXT.ownerSearch}
            aria-invalid={Boolean(error)}
            onChange={(event) => setDraft(event.target.value)}
            className={joinClassNames(CONTROL_CLASS_NAME, error ? "border-red-300" : "border-gray-200")}
          />
          <ul className={joinClassNames("flex max-h-48 flex-col overflow-y-auto rounded-lg border border-gray-100", isFetching && "opacity-60")}>
            {data?.content.length === 0 && <li className="px-3 py-2 text-sm text-gray-500">{SAVE_VEHICLE_TEXT.ownerNotFound}</li>}
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
