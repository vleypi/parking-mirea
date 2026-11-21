"use client";

import { joinClassNames } from "@/shared/lib";
import { Icon } from "@/shared/ui/Icon";
import { SIDEBAR_TEXT } from "@/shared/ui/sidebar/sidebar.constant";
import { useSidebar } from "@/shared/ui/sidebar/useSidebar";

interface ISidebarToggleProps {
  className?: string;
}

export const SidebarToggle = ({ className }: ISidebarToggleProps) => {
  const { toggle } = useSidebar();

  return (
    <button
      type="button"
      onClick={toggle}
      aria-label={SIDEBAR_TEXT.toggleMenu}
      className={joinClassNames("rounded-lg px-3 py-2 text-gray-700 transition hover:bg-gray-100", className)}
    >
      <Icon name="menu" />
    </button>
  );
};
