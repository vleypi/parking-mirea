export interface IAuthCookies {
  access: string;
  refresh: string;
}

export interface IAuthProxyConfig {
  cookies: IAuthCookies;
  sessionPath: string;
  refreshPath: string;
  loginRoute: string;
  homeRoute: string;
  guestOnlyRoutes: readonly string[];
  protectedRoutes: readonly string[];
}

export type SessionState = { status: "authenticated"; setCookies: string[] } | { status: "anonymous" } | { status: "unavailable" };
