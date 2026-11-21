"use client";

import { type MouseEvent, type ReactNode, useId } from "react";
import { joinClassNames } from "@/shared/lib/join-class-names";
import { Icon } from "@/shared/ui/Icon";
import { UI_TEXT } from "@/shared/ui/ui.constant";
import { useDialogVisibility } from "@/shared/ui/useDialogVisibility";

type ModalSize = "md" | "lg" | "xl";

interface IModalProps {
  isOpen: boolean;
  title: string;
  size?: ModalSize;
  onClose: () => void;
  children: ReactNode;
}

const SIZE_CLASSES: Record<ModalSize, string> = {
  md: "max-w-lg",
  lg: "max-w-2xl",
  xl: "max-w-6xl",
};

export const Modal = ({ isOpen, title, size = "md", onClose, children }: IModalProps) => {
  const dialogRef = useDialogVisibility(isOpen);
  const titleId = useId();

  const handleBackdropClick = (event: MouseEvent<HTMLDialogElement>) => {
    if (event.target === event.currentTarget) onClose();
  };

  return (
    <dialog
      ref={dialogRef}
      onClose={onClose}
      onClick={handleBackdropClick}
      aria-labelledby={titleId}
      className={joinClassNames(
        "m-auto max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] scale-95 overflow-hidden rounded-2xl bg-white p-0 opacity-0 shadow-2xl transition-all transition-discrete duration-200 ease-out-expo backdrop:bg-black/40 backdrop:opacity-0 backdrop:transition-all backdrop:transition-discrete backdrop:duration-200 open:scale-100 open:opacity-100 open:backdrop:opacity-100 starting:open:scale-95 starting:open:opacity-0 starting:open:backdrop:opacity-0",
        SIZE_CLASSES[size],
      )}
    >
      <div className="flex max-h-[calc(100dvh-2rem)] flex-col">
        <div className="flex shrink-0 items-center justify-between border-b border-gray-100 px-6 py-4">
          <h2 id={titleId} className="text-base font-semibold text-ink">
            {title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label={UI_TEXT.close}
            className="grid size-8 place-items-center rounded-lg text-gray-500 transition hover:bg-gray-100 hover:text-ink"
          >
            <Icon name="close" className="size-4" />
          </button>
        </div>
        <div className="overflow-y-auto px-6 pt-6">{isOpen && children}</div>
      </div>
    </dialog>
  );
};
