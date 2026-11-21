import { LOGO_TEXT } from "@/shared/ui/ui.constant";

export const Logo = () => (
  <span className="flex shrink-0 items-center gap-2">
    <span aria-hidden className="grid size-8 place-items-center rounded-lg bg-brand font-display text-lg font-bold text-white">
      {LOGO_TEXT.mark}
    </span>
    <span className="font-display text-base font-semibold text-ink">{LOGO_TEXT.label}</span>
  </span>
);
