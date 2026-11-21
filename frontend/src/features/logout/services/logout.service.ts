import { apiClient } from "@/shared/api";
import { AUTH_API_PATHS } from "@/shared/config";

class LogoutService {
  async logout(): Promise<void> {
    await apiClient.post(AUTH_API_PATHS.logout);
  }
}

export const logoutService = new LogoutService();
