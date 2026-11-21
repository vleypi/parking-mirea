import { ROUTES } from "@/shared/config";

export const resolveRedirect = (next: string | undefined): string =>
  next?.startsWith("/") && !next.startsWith("//") && !next.startsWith(ROUTES.login) ? next : ROUTES.home;
