import type { ReactNode } from "react";
import { Button } from "@/shared/ui/Button";
import { UI_TEXT } from "@/shared/ui/ui.constant";

interface IBulkActionBarProps {
  count: number;
  onClear: () => void;
  children: ReactNode;
}

export const BulkActionBar = ({ count, onClear, children }: IBulkActionBarProps) =>
  count > 0 ? (
    <div className="sticky top-0 z-10 mb-4 flex flex-wrap items-center gap-2 rounded-xl border border-brand/20 bg-brand-soft px-4 py-2">
      <span className="text-sm font-medium text-brand tabular-nums">
        {UI_TEXT.selected}: {count}
      </span>
      <div className="flex flex-wrap items-center gap-2">{children}</div>
      <Button variant="ghost" onClick={onClear} className="ml-auto h-8 px-3">
        {UI_TEXT.clearSelection}
      </Button>
    </div>
  ) : null;
