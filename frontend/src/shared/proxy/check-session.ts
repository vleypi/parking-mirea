import type { NextRequest } from "next/server";
import { API_URL } from "@/shared/config/api-url.constant";
import { API_BASE_PATH } from "@/shared/api/api.constant";
import type { IAuthProxyConfig, SessionState } from "@/shared/proxy/auth-proxy.interface";

const SESSION_CHECK_TIMEOUT_MS = 5000;

const callApi = (path: string, method: "GET" | "POST", cookie: string) =>
  fetch(`${API_URL}${API_BASE_PATH}${path}`, {
    method,
    headers: { cookie },
    cache: "no-store",
    signal: AbortSignal.timeout(SESSION_CHECK_TIMEOUT_MS),
  });

export const checkSession = async (request: NextRequest, { sessionPath, refreshPath, cookies }: IAuthProxyConfig): Promise<SessionState> => {
  const cookie = request.headers.get("cookie") ?? "";

  try {
    if ((await callApi(sessionPath, "GET", cookie)).ok) return { status: "authenticated", setCookies: [] };
    if (!request.cookies.has(cookies.refresh)) return { status: "anonymous" };

    const refreshed = await callApi(refreshPath, "POST", cookie);
    return refreshed.ok ? { status: "authenticated", setCookies: refreshed.headers.getSetCookie() } : { status: "anonymous" };
  } catch {
    return { status: "unavailable" };
  }
};
