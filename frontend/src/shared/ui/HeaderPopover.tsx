import { joinClassNames } from "@/shared/lib";
import type { ReactNode, Ref } from "react";
import { POPOVER_PANEL_CLASS_NAME } from "@/shared/ui/ui.constant";

interface IHeaderPopoverProps {
  id: string;
  className: string;
  ref?: Ref<HTMLDivElement>;
  children: ReactNode;
}

export const HeaderPopover = ({ id, className, ref, children }: IHeaderPopoverProps) => (
  <div
    ref={ref}
    id={id}
    popover="auto"
    className={joinClassNames(
      "fixed inset-auto top-header m-0 mt-2",
      POPOVER_PANEL_CLASS_NAME,
      className,
    )}
  >
    {children}
  </div>
);
