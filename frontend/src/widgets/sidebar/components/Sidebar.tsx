"use client";

import { SidebarShell, SidebarToggle } from "@/shared/ui";
import { getAvailableNavItems, useViewer } from "@/entities/viewer";
import { SidebarLink } from "@/widgets/sidebar/components/SidebarLink";
import { SIDEBAR_NAV_TEXT } from "@/widgets/sidebar/constants/sidebar.constant";

export const Sidebar = () => {
  const { data: viewer } = useViewer();

  return (
    <SidebarShell>
      <div className="-mt-2 mb-2 flex h-header shrink-0 items-center">
        <SidebarToggle />
      </div>
      <nav aria-label={SIDEBAR_NAV_TEXT.navigation} className="flex flex-col gap-1">
        {getAvailableNavItems(viewer).map((item) => (
          <SidebarLink key={item.href} item={item} />
        ))}
      </nav>
    </SidebarShell>
  );
};
