"use client";

import { joinClassNames } from "@/shared/lib";
import { Icon } from "@/shared/ui/Icon";
import { type ISelectOption } from "@/shared/ui/SelectControl";
import { useId } from "react";
import { type PopoverAlign, useAnchoredPopover } from "@/shared/lib";
import { MenuOptionList } from "@/shared/ui/MenuOptionList";
import { POPOVER_PANEL_CLASS_NAME, TOOLBAR_CONTROL_CLASS_NAME } from "@/shared/ui/ui.constant";

interface IToolbarMenuProps {
  label: string;
  options: readonly ISelectOption[];
  value: string;
  isApplied?: boolean;
  align?: PopoverAlign;
  onChange: (value: string) => void;
}

export const ToolbarMenu = ({ label, options, value, isApplied = false, align = "start", onChange }: IToolbarMenuProps) => {
  const menuId = useId();
  const { anchorRef, popoverRef } = useAnchoredPopover(align);
  const selected = options.find((option) => option.value === value) ?? options[0];

  return (
    <>
      <button
        ref={anchorRef}
        type="button"
        popoverTarget={menuId}
        aria-label={`${label}: ${selected?.label ?? ""}`}
        className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "inline-flex shrink-0 items-center gap-2 pr-2.5 pl-3", isApplied && "font-semibold ring-1 ring-brand/30")}
      >
        <span className="truncate">{selected?.label}</span>
        <Icon name="chevronDown" className="size-3.5 text-gray-500" />
      </button>
      <div ref={popoverRef} id={menuId} popover="auto" className={joinClassNames("fixed inset-auto m-0 min-w-56 p-1.5", POPOVER_PANEL_CLASS_NAME)}>
        <MenuOptionList menuId={menuId} options={options} value={value} onSelect={onChange} />
      </div>
    </>
  );
};
