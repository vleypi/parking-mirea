"use client";

import { writeCookie } from "@/shared/lib";
import { type ReactNode, useState } from "react";
import { COOKIE_KEYS } from "@/shared/config";
import { DESKTOP_MEDIA_QUERY } from "@/shared/ui/sidebar/sidebar.constant";
import { SidebarContext } from "@/shared/ui/sidebar/sidebar.context";

interface ISidebarProviderProps {
  initialExpanded: boolean;
  children: ReactNode;
}

export const SidebarProvider = ({ initialExpanded, children }: ISidebarProviderProps) => {
  const [isExpanded, setIsExpanded] = useState(initialExpanded);
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  const toggle = () => {
    if (!window.matchMedia(DESKTOP_MEDIA_QUERY).matches) {
      setIsMobileOpen((isOpen) => !isOpen);
      return;
    }

    writeCookie(COOKIE_KEYS.sidebarOpen, String(!isExpanded));
    setIsExpanded(!isExpanded);
  };

  const closeMobile = () => setIsMobileOpen(false);

  return <SidebarContext value={{ isExpanded, isMobileOpen, toggle, closeMobile }}>{children}</SidebarContext>;
};
