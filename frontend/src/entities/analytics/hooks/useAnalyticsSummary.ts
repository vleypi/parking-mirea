"use client";

import { useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { analyticsService } from "@/entities/analytics/services/analytics.service";

export const useAnalyticsSummary = () => useQuery({ queryKey: QUERY_KEYS.analytics, queryFn: () => analyticsService.getSummary() });
