"use client";

import { formatDateTime, formatMoney } from "@/shared/lib";
import { Badge, FormError, SectionHeader } from "@/shared/ui";
import { OWNER_TEXT } from "@/entities/owner/constants/owner.constant";
import { useOwnerRelated } from "@/entities/owner/hooks/useOwner";

interface IOwnerRelatedListProps {
  ownerId: string;
}

const LOAD_FAILED = "Не удалось загрузить машины и заявки";

export const OwnerRelatedList = ({ ownerId }: IOwnerRelatedListProps) => {
  const { data, isPending, isError } = useOwnerRelated(ownerId);

  if (isPending) return <div className="h-24 animate-pulse rounded-xl bg-gray-100" />;
  if (isError) return <FormError message={LOAD_FAILED} />;

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <SectionHeader title={OWNER_TEXT.vehiclesTitle} description={OWNER_TEXT.vehiclesDescription} />
        {data.vehicles.length === 0 && <p className="text-sm text-gray-500">{OWNER_TEXT.noVehicles}</p>}
        <ul className="flex flex-wrap gap-2">
          {data.vehicles.map((vehicle) => (
            <li key={vehicle.id} className="rounded-lg bg-canvas px-3 py-2 text-sm">
              <span className="font-medium text-ink">{vehicle.licensePlate}</span>{" "}
              <span className="text-gray-500">
                {vehicle.brand} {vehicle.model}
              </span>
            </li>
          ))}
        </ul>
      </section>
      <section className="flex flex-col gap-2">
        <SectionHeader
          title={OWNER_TEXT.requestsTitle}
          description={
            data.requestsTotal > data.requests.length
              ? `${OWNER_TEXT.requestsShown} ${data.requests.length} ${OWNER_TEXT.requestsOf} ${data.requestsTotal}`
              : OWNER_TEXT.requestsDescription
          }
        />
        {data.requests.length === 0 && <p className="text-sm text-gray-500">{OWNER_TEXT.noRequests}</p>}
        <ul className="flex flex-col divide-y divide-gray-100">
          {data.requests.map((request) => (
            <li key={request.id} className="flex flex-wrap items-center justify-between gap-2 py-2 text-sm">
              <span>
                <span className="font-medium text-ink">{request.code}</span>{" "}
                <span className="text-gray-500">
                  {request.licensePlate}, {OWNER_TEXT.spot} {request.spotNumber}, {formatDateTime(request.startTime)}
                </span>
              </span>
              <span className="flex items-center gap-2">
                <span className="tabular-nums">{formatMoney(request.cost)}</span>
                <Badge>{request.statusTitle}</Badge>
                {request.archived && <Badge>{OWNER_TEXT.archived}</Badge>}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
};
