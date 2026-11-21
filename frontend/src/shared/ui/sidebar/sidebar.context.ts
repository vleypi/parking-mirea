import { createContext } from "react";

export interface ISidebarState {
  isExpanded: boolean;
  isMobileOpen: boolean;
  toggle: () => void;
  closeMobile: () => void;
}

export const SidebarContext = createContext<ISidebarState | null>(null);
