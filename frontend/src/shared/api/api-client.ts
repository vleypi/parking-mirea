import { createApiClient } from "@/shared/api/create-api-client";
import { AUTH_API_PATHS, ROUTES } from "@/shared/config";

export const apiClient = createApiClient({
  refreshPath: AUTH_API_PATHS.refresh,
  loginRoute: ROUTES.login,
  ownAuthErrorPaths: [AUTH_API_PATHS.login],
});
