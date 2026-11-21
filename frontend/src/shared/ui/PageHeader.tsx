import type { ReactNode } from "react";
import { AddButton, type IAddAction } from "@/shared/ui/AddButton";

interface IPageHeaderProps {
  title: string;
  description?: string;
  add?: IAddAction;
  actions?: ReactNode;
}

export const PageHeader = ({ title, description, add, actions }: IPageHeaderProps) => (
  <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
    <div>
      <h1 className="font-display text-4xl font-medium tracking-tight text-ink">{title}</h1>
      {description && <p className="mt-2 text-[15px] text-gray-500">{description}</p>}
    </div>
    {(add || actions) && (
      <div className="flex items-center gap-2">
        {actions}
        {add && <AddButton label={add.label} onClick={add.onClick} disabled={add.disabled} />}
      </div>
    )}
  </div>
);
