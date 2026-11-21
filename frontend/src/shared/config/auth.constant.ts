import { ROUTES } from "@/shared/config/routes.constant";

export const AUTH_COOKIES = {
  access: "parking_access",
  refresh: "parking_refresh",
} as const;

export const AUTH_API_PATHS = {
  login: "/auth/login",
  logout: "/auth/logout",
  refresh: "/auth/refresh",
  session: "/auth/session",
  me: "/auth/me",
  password: "/auth/change-password",
} as const;

export const GUEST_ONLY_ROUTES = [ROUTES.login] as const;
export const PROTECTED_ROUTES = [ROUTES.home] as const;
