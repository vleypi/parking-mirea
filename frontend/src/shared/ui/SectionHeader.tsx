import type { ReactNode } from "react";

interface ISectionHeaderProps {
  title: string;
  description?: string;
  actions?: ReactNode;
}

export const SectionHeader = ({ title, actions }: ISectionHeaderProps) => (
  <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
    <h2 className="font-display text-xl font-medium text-ink">{title}</h2>
    {actions && <div className="flex items-center gap-2">{actions}</div>}
  </div>
);
