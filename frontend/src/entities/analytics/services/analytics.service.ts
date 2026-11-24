import { apiClient } from "@/shared/api";
import type { IAnalyticsSummary } from "@/entities/analytics/interfaces/analytics.interface";

class AnalyticsService {
  async getSummary(): Promise<IAnalyticsSummary> {
    const { data } = await apiClient.get<IAnalyticsSummary>("/analytics/summary");
    return data;
  }
}

export const analyticsService = new AnalyticsService();
