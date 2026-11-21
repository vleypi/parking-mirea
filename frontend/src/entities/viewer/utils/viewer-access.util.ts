import { type INavItem, NAV_ITEMS, type Role, ROLE_RANK } from "@/shared/config";
import type { IViewer } from "@/entities/viewer/interfaces/viewer.interface";

export const getTopRole = (viewer: IViewer): Role =>
  viewer.roles.reduce((top, role) => (ROLE_RANK[role] > ROLE_RANK[top] ? role : top));

export const hasRole = (viewer: IViewer | undefined, minRole: Role): boolean =>
  Boolean(viewer?.roles.some((role) => ROLE_RANK[role] >= ROLE_RANK[minRole]));

export const getAvailableNavItems = (viewer: IViewer | undefined): INavItem[] => NAV_ITEMS.filter(({ minRole }) => hasRole(viewer, minRole));
