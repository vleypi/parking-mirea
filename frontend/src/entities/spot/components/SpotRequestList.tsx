"use client";

import { formatDateTime, formatMoney } from "@/shared/lib";
import { Badge, FormError, SectionHeader } from "@/shared/ui";
import { SPOT_TEXT } from "@/entities/spot/constants/spot.constant";
import { useSpotRequests } from "@/entities/spot/hooks/useSpot";

interface ISpotRequestListProps {
  spotId: string;
}

export const SpotRequestList = ({ spotId }: ISpotRequestListProps) => {
  const { data, isPending, isError } = useSpotRequests(spotId);

  if (isPending) return <div className="h-24 animate-pulse rounded-xl bg-gray-100" />;
  if (isError) return <FormError message={SPOT_TEXT.loadRequestsFailed} />;

  return (
    <section className="flex flex-col gap-2">
      <SectionHeader
        title={SPOT_TEXT.requestsTitle}
        description={
          data.total > data.requests.length
            ? `${SPOT_TEXT.requestsShown} ${data.requests.length} ${SPOT_TEXT.requestsOf} ${data.total}`
            : SPOT_TEXT.requestsDescription
        }
      />
      {data.requests.length === 0 && <p className="text-sm text-gray-500">{SPOT_TEXT.noRequests}</p>}
      <ul className="flex flex-col divide-y divide-gray-100">
        {data.requests.map((request) => (
          <li key={request.id} className="flex flex-wrap items-center justify-between gap-2 py-2 text-sm">
            <span>
              <span className="font-medium text-ink">{request.code}</span>{" "}
              <span className="text-gray-500">
                {request.ownerName}, {request.licensePlate}, {formatDateTime(request.startTime)} – {formatDateTime(request.endTime)}
              </span>
            </span>
            <span className="flex items-center gap-2">
              <span className="tabular-nums">{formatMoney(request.cost)}</span>
              <Badge>{request.statusTitle}</Badge>
              {request.archived && <Badge>{SPOT_TEXT.archived}</Badge>}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
};
