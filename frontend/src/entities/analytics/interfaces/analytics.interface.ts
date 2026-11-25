export type AnalyticsGroups = Record<string, number>;

export interface IRequestAnalytics {
  total: number;
  active: number;
  archived: number;
  paid: number;
  unpaid: number;
  averageCost: number | null;
  byStatus: AnalyticsGroups;
  bySpotType: AnalyticsGroups;
  byPriority: AnalyticsGroups;
}

export interface IOwnerAnalytics {
  total: number;
  active: number;
  archived: number;
  notificationsEnabled: number;
  averageDiscount: number | null;
  byCategory: AnalyticsGroups;
  byStatus: AnalyticsGroups;
  byLoyalty: AnalyticsGroups;
}

export interface IVehicleAnalytics {
  total: number;
  active: number;
  archived: number;
  electric: number;
  averageReleaseYear: number | null;
  byBrand: AnalyticsGroups;
  byStatus: AnalyticsGroups;
  byActivity: AnalyticsGroups;
}

export interface ISpotAnalytics {
  total: number;
  active: number;
  archived: number;
  covered: number;
  averageHourlyRate: number | null;
  byType: AnalyticsGroups;
  byStatus: AnalyticsGroups;
  byAvailability: AnalyticsGroups;
}

export interface IAnalyticsSummary {
  requests: IRequestAnalytics;
  owners: IOwnerAnalytics;
  vehicles: IVehicleAnalytics;
  spots: ISpotAnalytics;
}
