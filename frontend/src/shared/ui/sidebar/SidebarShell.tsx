"use client";

import { joinClassNames } from "@/shared/lib";
import type { ReactNode } from "react";
import { SIDEBAR_TEXT } from "@/shared/ui/sidebar/sidebar.constant";
import { useSidebar } from "@/shared/ui/sidebar/useSidebar";

interface ISidebarShellProps {
  children: ReactNode;
}

export const SidebarShell = ({ children }: ISidebarShellProps) => {
  const { isExpanded, isMobileOpen, closeMobile } = useSidebar();

  return (
    <>
      <button
        type="button"
        aria-label={SIDEBAR_TEXT.closeMenu}
        onClick={closeMobile}
        className={joinClassNames(
          "fixed inset-0 z-40 bg-black/40 transition-opacity duration-300 md:hidden",
          isMobileOpen ? "opacity-100" : "pointer-events-none opacity-0",
        )}
      />
      <aside
        data-expanded={isExpanded}
        className={joinClassNames(
          "group fixed inset-y-0 left-0 z-50 flex w-sidebar shrink-0 flex-col overflow-hidden bg-white transition-[translate,width] duration-300 ease-out-expo",
          "md:static md:z-auto md:translate-x-0",
          isMobileOpen ? "translate-x-0 shadow-xl" : "-translate-x-full",
          isExpanded ? "md:w-sidebar" : "md:w-sidebar-collapsed",
        )}
      >
        <div className="flex h-full flex-col overflow-y-auto p-4">{children}</div>
      </aside>
    </>
  );
};
