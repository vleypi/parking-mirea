"use client";

import { Logo, SidebarToggle } from "@/shared/ui";
import { ViewerMenu } from "@/widgets/app-header/components/ViewerMenu";

export const AppHeader = () => (
  <header className="flex h-header shrink-0 items-center justify-between gap-4 px-4 md:px-8">
    <div className="flex shrink-0 items-center gap-3">
      <SidebarToggle className="md:hidden" />
      <Logo />
    </div>
    <ViewerMenu />
  </header>
);
