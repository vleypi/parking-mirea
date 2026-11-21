import { createAuthProxy } from "@/shared/proxy";
import { AUTH_API_PATHS, AUTH_COOKIES, GUEST_ONLY_ROUTES, PROTECTED_ROUTES, ROUTES } from "@/shared/config";

export const proxy = createAuthProxy({
  cookies: AUTH_COOKIES,
  sessionPath: AUTH_API_PATHS.session,
  refreshPath: AUTH_API_PATHS.refresh,
  loginRoute: ROUTES.login,
  homeRoute: ROUTES.home,
  guestOnlyRoutes: GUEST_ONLY_ROUTES,
  protectedRoutes: PROTECTED_ROUTES,
});

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\.svg$).*)"],
};
