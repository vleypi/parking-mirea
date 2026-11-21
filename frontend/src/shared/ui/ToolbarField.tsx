"use client";

import { joinClassNames } from "@/shared/lib/join-class-names";
import type { IListField } from "@/shared/lib/list.interface";
import { useDebouncedDraft } from "@/shared/lib/useDebouncedDraft";
import { TOOLBAR_CONTROL_CLASS_NAME } from "@/shared/ui/ui.constant";

interface IToolbarFieldProps {
  field: IListField;
  value: string;
  onChange: (value: string) => void;
}

export const ToolbarField = ({ field, value, onChange }: IToolbarFieldProps) => {
  const { draft, setDraft } = useDebouncedDraft(value, onChange);

  return (
    <label
      className={joinClassNames(
        TOOLBAR_CONTROL_CLASS_NAME,
        "inline-flex shrink-0 items-center gap-2 px-3 focus-within:border-brand focus-within:ring-2 focus-within:ring-brand/15",
        value !== "" && "font-semibold ring-1 ring-brand/30",
      )}
    >
      <span className="text-gray-500">{field.label}</span>
      <input
        type={field.type}
        value={draft}
        aria-label={field.label}
        onChange={(event) => setDraft(event.target.value)}
        className={joinClassNames("bg-transparent text-ink outline-none", field.type === "datetime-local" ? "w-44" : "w-24")}
      />
    </label>
  );
};
