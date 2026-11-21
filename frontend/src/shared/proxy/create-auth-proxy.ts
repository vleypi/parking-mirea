import { type NextRequest, NextResponse } from "next/server";
import type { IAuthProxyConfig } from "@/shared/proxy/auth-proxy.interface";
import { checkSession } from "@/shared/proxy/check-session";

const NEXT_PARAM = "next";

const matchesAny = (pathname: string, routes: readonly string[]) =>
  routes.some((route) => route === "/" || pathname === route || pathname.startsWith(`${route}/`));

const redirectToLogin = (request: NextRequest, { loginRoute }: IAuthProxyConfig) => {
  const url = new URL(loginRoute, request.url);
  url.searchParams.set(NEXT_PARAM, `${request.nextUrl.pathname}${request.nextUrl.search}`);

  return NextResponse.redirect(url);
};

export const createAuthProxy = (config: IAuthProxyConfig) => async (request: NextRequest) => {
  const { pathname } = request.nextUrl;
  const isGuestOnly = matchesAny(pathname, config.guestOnlyRoutes);
  const isProtected = !isGuestOnly && matchesAny(pathname, config.protectedRoutes);
  const hasSessionCookie = request.cookies.has(config.cookies.access) || request.cookies.has(config.cookies.refresh);

  if (!hasSessionCookie) return isProtected ? redirectToLogin(request, config) : NextResponse.next();

  const session = await checkSession(request, config);
  if (session.status === "unavailable") return NextResponse.next();

  if (session.status === "authenticated") {
    const response = isGuestOnly ? NextResponse.redirect(new URL(config.homeRoute, request.url)) : NextResponse.next();
    session.setCookies.forEach((cookie) => response.headers.append("set-cookie", cookie));
    return response;
  }

  const response = isProtected ? redirectToLogin(request, config) : NextResponse.next();
  response.cookies.delete(config.cookies.access);
  response.cookies.delete(config.cookies.refresh);
  return response;
};
