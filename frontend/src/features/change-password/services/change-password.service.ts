import { apiClient } from "@/shared/api";
import { AUTH_API_PATHS } from "@/shared/config";
import type { ChangePasswordFormValues } from "@/features/change-password/schemas/change-password.schema";

class ChangePasswordService {
  async change({ currentPassword, newPassword }: ChangePasswordFormValues): Promise<void> {
    await apiClient.post(AUTH_API_PATHS.password, { currentPassword, newPassword });
  }
}

export const changePasswordService = new ChangePasswordService();
