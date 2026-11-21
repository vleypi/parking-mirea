"use client";

import { useEffect, useRef } from "react";

export type PopoverAlign = "start" | "end";

const POPOVER_GAP = 6;

export const useAnchoredPopover = (align: PopoverAlign) => {
  const anchorRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const popover = popoverRef.current;
    if (!popover) return;

    const place = (event: Event) => {
      const anchor = anchorRef.current;
      if (!anchor || !(event instanceof ToggleEvent) || event.newState !== "open") return;

      const { left, right, bottom } = anchor.getBoundingClientRect();
      popover.style.top = `${bottom + POPOVER_GAP}px`;
      popover.style.left = align === "start" ? `${left}px` : "auto";
      popover.style.right = align === "end" ? `${window.innerWidth - right}px` : "auto";
    };

    popover.addEventListener("beforetoggle", place);
    return () => popover.removeEventListener("beforetoggle", place);
  }, [align]);

  return { anchorRef, popoverRef };
};
