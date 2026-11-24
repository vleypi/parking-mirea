"use client";

import { ROLES } from "@/shared/config";
import { RequireRole } from "@/entities/viewer";
import { AnalyticsOverview } from "@/views/analytics/components/AnalyticsOverview";

export const AnalyticsPage = () => (
  <RequireRole minRole={ROLES.viewer}>
    <AnalyticsOverview />
  </RequireRole>
);
