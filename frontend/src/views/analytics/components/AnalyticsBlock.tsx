import { CARD_CLASS_NAME, SectionHeader } from "@/shared/ui";
import type { AnalyticsGroups } from "@/entities/analytics";
import { ANALYTICS_PAGE_TEXT } from "@/views/analytics/constants/analytics-page.constant";

export interface IAnalyticsStat {
  label: string;
  value: string | number;
}

export interface IAnalyticsGroup {
  title: string;
  groups: AnalyticsGroups;
  labels?: Readonly<Record<string, string>>;
}

interface IAnalyticsBlockProps {
  title: string;
  stats: readonly IAnalyticsStat[];
  groups: readonly IAnalyticsGroup[];
}

const GroupTable = ({ title, groups, labels }: IAnalyticsGroup) => {
  const rows = Object.entries(groups);
  const total = rows.reduce((sum, [, count]) => sum + count, 0);

  return (
    <div className={CARD_CLASS_NAME}>
      <h3 className="mb-3 text-sm font-medium text-ink">{title}</h3>
      {rows.length === 0 && <p className="text-sm text-gray-500">{ANALYTICS_PAGE_TEXT.noData}</p>}
      <ul className="flex flex-col gap-2 text-sm">
        {rows.map(([key, count]) => (
          <li key={key} className="flex flex-col gap-1">
            <span className="flex justify-between gap-3">
              <span>{labels?.[key] ?? key}</span>
              <span className="tabular-nums text-gray-600">{count}</span>
            </span>
            <span className="h-1.5 overflow-hidden rounded-full bg-gray-100">
              <span className="block h-full rounded-full bg-brand" style={{ width: total > 0 ? `${(count / total) * 100}%` : 0 }} />
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
};

/** Блок сводки: карточки с числами и таблицы групп с долей каждой группы. */
export const AnalyticsBlock = ({ title, stats, groups }: IAnalyticsBlockProps) => (
  <section className="mb-10">
    <SectionHeader title={title} description="" />
    <dl className="mb-4 grid gap-3 sm:grid-cols-3 lg:grid-cols-6">
      {stats.map(({ label, value }) => (
        <div key={label} className={CARD_CLASS_NAME}>
          <dt className="text-xs text-gray-500">{label}</dt>
          <dd className="mt-1 text-2xl font-medium tabular-nums text-ink">{value}</dd>
        </div>
      ))}
    </dl>
    <div className="grid gap-4 md:grid-cols-3">
      {groups.map((group) => (
        <GroupTable key={group.title} {...group} />
      ))}
    </div>
  </section>
);
