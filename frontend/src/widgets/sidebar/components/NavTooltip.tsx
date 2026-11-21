import type { Ref } from "react";

interface INavTooltipProps {
  label: string;
  ref: Ref<HTMLSpanElement>;
}

export const NavTooltip = ({ label, ref }: INavTooltipProps) => (
  <span
    ref={ref}
    popover="manual"
    aria-hidden
    className="pointer-events-none fixed inset-auto m-0 -translate-y-1/2 overflow-visible rounded-lg border-0 bg-mint px-3 py-1.5 text-sm font-medium whitespace-nowrap text-forest before:absolute before:top-1/2 before:-left-1 before:size-2 before:-translate-y-1/2 before:rotate-45 before:bg-mint max-md:hidden"
  >
    {label}
  </span>
);
