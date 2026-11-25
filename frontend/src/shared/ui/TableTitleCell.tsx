import type { ReactNode } from "react";
import { MarqueeText } from "@/shared/ui/MarqueeText";

interface ITableTitleCellProps {
  title: ReactNode;
  subtitle?: ReactNode;
}

export const TableTitleCell = ({ title, subtitle }: ITableTitleCellProps) => (
  <div>
    <p className="font-display text-base font-medium text-ink">
      <MarqueeText className="max-w-[13rem]">{title}</MarqueeText>
    </p>
    {subtitle && (
      <p className="text-sm text-gray-500">
        <MarqueeText className="max-w-[13rem]">{subtitle}</MarqueeText>
      </p>
    )}
  </div>
);
