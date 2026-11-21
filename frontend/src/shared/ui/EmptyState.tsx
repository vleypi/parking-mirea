import { Icon } from "@/shared/ui/Icon";
import { type IconName } from "@/shared/ui/icons.constant";
import type { ReactNode } from "react";

interface IEmptyStateProps {
  icon: IconName;
  title: string;
  description: string;
  action?: ReactNode;
}

export const EmptyState = ({ icon, title, description, action }: IEmptyStateProps) => (
  <div className="flex flex-col items-center px-4 py-16 text-center">
    <Icon name={icon} className="mb-3 size-8 text-gray-400" />
    <h2 className="text-base font-semibold text-ink">{title}</h2>
    <p className="mt-1 max-w-sm text-sm text-gray-500">{description}</p>
    {action && <div className="mt-6">{action}</div>}
  </div>
);
