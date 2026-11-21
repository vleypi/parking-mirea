import { apiClient } from "@/shared/api";
import { AUTH_API_PATHS } from "@/shared/config";
import type { IViewer } from "@/entities/viewer";
import type { LoginFormValues } from "@/features/login/schemas/login.schema";

class LoginService {
  async login(credentials: LoginFormValues): Promise<IViewer> {
    const { data } = await apiClient.post<IViewer>(AUTH_API_PATHS.login, credentials);
    return data;
  }
}

export const loginService = new LoginService();
