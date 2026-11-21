import type { IconName } from "@/shared/ui";
import { type Role, ROLES } from "@/shared/config/role.constant";
import { ROUTES } from "@/shared/config/routes.constant";

export interface INavItem {
  href: string;
  label: string;
  icon: IconName;
  minRole: Role;
}

export const NAV_ITEMS: readonly INavItem[] = [
  { href: ROUTES.requests, label: "Заявки", icon: "calendar", minRole: ROLES.viewer },
  { href: ROUTES.owners, label: "Владельцы", icon: "users", minRole: ROLES.viewer },
  { href: ROUTES.vehicles, label: "Автомобили", icon: "car", minRole: ROLES.viewer },
  { href: ROUTES.spots, label: "Места", icon: "grid", minRole: ROLES.viewer },
  { href: ROUTES.analytics, label: "Аналитика", icon: "chart", minRole: ROLES.viewer },
  { href: ROUTES.references, label: "Справочники", icon: "book", minRole: ROLES.viewer },
  { href: ROUTES.users, label: "Пользователи", icon: "lock", minRole: ROLES.admin },
  { href: ROUTES.audit, label: "Аудит", icon: "clipboard", minRole: ROLES.admin },
];
