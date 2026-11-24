"use client";

import { formatMoney } from "@/shared/lib";
import { FormError, PageHeader } from "@/shared/ui";
import { useAnalyticsSummary } from "@/entities/analytics";
import { LOYALTY_LABELS, OWNER_STATUS_LABELS } from "@/entities/owner";
import { type ICodeTitle, useReferences } from "@/entities/reference";
import { REQUEST_PRIORITY_LABELS } from "@/entities/request";
import { SPOT_AVAILABILITY_LABELS, SPOT_STATUS_LABELS } from "@/entities/spot";
import { VEHICLE_ACTIVITY_LABELS, VEHICLE_STATUS_LABELS } from "@/entities/vehicle";
import { AnalyticsBlock } from "@/views/analytics/components/AnalyticsBlock";
import { ANALYTICS_PAGE_TEXT } from "@/views/analytics/constants/analytics-page.constant";

const toLabels = (items: readonly ICodeTitle[] = []): Record<string, string> => Object.fromEntries(items.map(({ code, title }) => [code, title]));

const formatAverage = (value: number | null, format: (value: number) => string = String) => (value === null ? "—" : format(value));

/** Общая аналитика GET /api/analytics/summary: коды групп переводим в названия по справочникам. */
export const AnalyticsOverview = () => {
  const { data, isPending, isError } = useAnalyticsSummary();
  const { data: references } = useReferences();

  return (
    <>
      <PageHeader title={ANALYTICS_PAGE_TEXT.title} description={ANALYTICS_PAGE_TEXT.description} />
      {isError && <FormError message={ANALYTICS_PAGE_TEXT.loadFailed} />}
      {isPending && <div className="h-64 animate-pulse rounded-xl bg-gray-100" />}
      {data && (
        <>
          <AnalyticsBlock
            title={ANALYTICS_PAGE_TEXT.requests}
            stats={[
              { label: ANALYTICS_PAGE_TEXT.total, value: data.requests.total },
              { label: ANALYTICS_PAGE_TEXT.active, value: data.requests.active },
              { label: ANALYTICS_PAGE_TEXT.archived, value: data.requests.archived },
              { label: ANALYTICS_PAGE_TEXT.paid, value: data.requests.paid },
              { label: ANALYTICS_PAGE_TEXT.unpaid, value: data.requests.unpaid },
              { label: ANALYTICS_PAGE_TEXT.averageCost, value: formatAverage(data.requests.averageCost, formatMoney) },
            ]}
            groups={[
              { title: ANALYTICS_PAGE_TEXT.byStatus, groups: data.requests.byStatus, labels: toLabels(references?.requestStatuses) },
              { title: ANALYTICS_PAGE_TEXT.bySpotType, groups: data.requests.bySpotType, labels: toLabels(references?.spotTypes) },
              { title: ANALYTICS_PAGE_TEXT.byPriority, groups: data.requests.byPriority, labels: REQUEST_PRIORITY_LABELS },
            ]}
          />
          <AnalyticsBlock
            title={ANALYTICS_PAGE_TEXT.owners}
            stats={[
              { label: ANALYTICS_PAGE_TEXT.total, value: data.owners.total },
              { label: ANALYTICS_PAGE_TEXT.active, value: data.owners.active },
              { label: ANALYTICS_PAGE_TEXT.archived, value: data.owners.archived },
              { label: ANALYTICS_PAGE_TEXT.notificationsEnabled, value: data.owners.notificationsEnabled },
              { label: ANALYTICS_PAGE_TEXT.averageDiscount, value: formatAverage(data.owners.averageDiscount, (value) => `${value}%`) },
            ]}
            groups={[
              { title: ANALYTICS_PAGE_TEXT.byCategory, groups: data.owners.byCategory, labels: toLabels(references?.clientCategories) },
              { title: ANALYTICS_PAGE_TEXT.byStatus, groups: data.owners.byStatus, labels: OWNER_STATUS_LABELS },
              { title: ANALYTICS_PAGE_TEXT.byLoyalty, groups: data.owners.byLoyalty, labels: LOYALTY_LABELS },
            ]}
          />
          <AnalyticsBlock
            title={ANALYTICS_PAGE_TEXT.vehicles}
            stats={[
              { label: ANALYTICS_PAGE_TEXT.total, value: data.vehicles.total },
              { label: ANALYTICS_PAGE_TEXT.active, value: data.vehicles.active },
              { label: ANALYTICS_PAGE_TEXT.archived, value: data.vehicles.archived },
              { label: ANALYTICS_PAGE_TEXT.electric, value: data.vehicles.electric },
              { label: ANALYTICS_PAGE_TEXT.averageReleaseYear, value: formatAverage(data.vehicles.averageReleaseYear) },
            ]}
            groups={[
              { title: ANALYTICS_PAGE_TEXT.byBrand, groups: data.vehicles.byBrand },
              { title: ANALYTICS_PAGE_TEXT.byStatus, groups: data.vehicles.byStatus, labels: VEHICLE_STATUS_LABELS },
              { title: ANALYTICS_PAGE_TEXT.byActivity, groups: data.vehicles.byActivity, labels: VEHICLE_ACTIVITY_LABELS },
            ]}
          />
          <AnalyticsBlock
            title={ANALYTICS_PAGE_TEXT.spots}
            stats={[
              { label: ANALYTICS_PAGE_TEXT.total, value: data.spots.total },
              { label: ANALYTICS_PAGE_TEXT.active, value: data.spots.active },
              { label: ANALYTICS_PAGE_TEXT.archived, value: data.spots.archived },
              { label: ANALYTICS_PAGE_TEXT.covered, value: data.spots.covered },
              { label: ANALYTICS_PAGE_TEXT.averageHourlyRate, value: formatAverage(data.spots.averageHourlyRate, formatMoney) },
            ]}
            groups={[
              { title: ANALYTICS_PAGE_TEXT.byType, groups: data.spots.byType },
              { title: ANALYTICS_PAGE_TEXT.byStatus, groups: data.spots.byStatus, labels: SPOT_STATUS_LABELS },
              { title: ANALYTICS_PAGE_TEXT.byAvailability, groups: data.spots.byAvailability, labels: SPOT_AVAILABILITY_LABELS },
            ]}
          />
        </>
      )}
    </>
  );
};
