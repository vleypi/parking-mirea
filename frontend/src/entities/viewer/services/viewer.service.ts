import { apiClient } from "@/shared/api";
import { AUTH_API_PATHS } from "@/shared/config";
import type { IViewer } from "@/entities/viewer/interfaces/viewer.interface";

class ViewerService {
  async getMe(): Promise<IViewer> {
    const { data } = await apiClient.get<IViewer>(AUTH_API_PATHS.me);
    return data;
  }
}

export const viewerService = new ViewerService();
