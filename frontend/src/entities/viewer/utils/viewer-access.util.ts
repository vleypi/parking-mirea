import { type INavItem, NAV_ITEMS, type Role } from "@/shared/config";
import { hasMinRole } from "@/shared/lib";
import type { IViewer } from "@/entities/viewer/interfaces/viewer.interface";

export const hasRole = (viewer: IViewer | undefined, minRole: Role): boolean => Boolean(viewer && hasMinRole(viewer.roles, minRole));

export const getAvailableNavItems = (viewer: IViewer | undefined): INavItem[] => NAV_ITEMS.filter(({ minRole }) => hasRole(viewer, minRole));
