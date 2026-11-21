import { useRef } from "react";
import { useSidebar } from "@/shared/ui";
import { TOOLTIP_GAP } from "@/widgets/sidebar/constants/sidebar.constant";

export const useNavTooltip = () => {
  const { isExpanded } = useSidebar();
  const anchorRef = useRef<HTMLAnchorElement>(null);
  const tooltipRef = useRef<HTMLSpanElement>(null);

  const show = () => {
    const anchor = anchorRef.current;
    const tooltip = tooltipRef.current;
    if (isExpanded || !anchor || !tooltip) return;

    const { right, top, height } = anchor.getBoundingClientRect();
    tooltip.style.left = `${right + TOOLTIP_GAP}px`;
    tooltip.style.top = `${top + height / 2}px`;
    tooltip.showPopover();
  };

  const hide = () => tooltipRef.current?.hidePopover();

  return { anchorRef, tooltipRef, show, hide };
};
