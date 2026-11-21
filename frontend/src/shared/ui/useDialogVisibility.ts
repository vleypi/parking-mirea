"use client";

import { useEffect, useRef } from "react";

const FIRST_FIELD_SELECTOR = "input:not([type=hidden]):not([type=file]):not([type=checkbox]):not([disabled]), select:not([disabled]), textarea:not([disabled])";

export const useDialogVisibility = (isOpen: boolean) => {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (isOpen && !dialog.open) {
      dialog.showModal();
      dialog.querySelector<HTMLElement>(FIRST_FIELD_SELECTOR)?.focus();
    }
    if (!isOpen && dialog.open) dialog.close();
  }, [isOpen]);

  return dialogRef;
};
