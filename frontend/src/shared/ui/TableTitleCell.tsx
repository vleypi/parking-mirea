import type { ReactNode } from "react";

interface ITableTitleCellProps {
  title: ReactNode;
  subtitle?: ReactNode;
}

export const TableTitleCell = ({ title, subtitle }: ITableTitleCellProps) => (
  <div>
    <p className="font-display text-base font-medium text-ink">{title}</p>
    {subtitle && <p className="text-sm text-gray-500">{subtitle}</p>}
  </div>
);
