"use client";

import { use } from "react";
import { type ISidebarState, SidebarContext } from "@/shared/ui/sidebar/sidebar.context";

export const useSidebar = (): ISidebarState => {
  const state = use(SidebarContext);
  if (!state) throw new Error("useSidebar используется вне SidebarProvider");

  return state;
};
