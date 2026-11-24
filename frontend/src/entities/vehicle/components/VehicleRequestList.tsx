"use client";

import { formatDateTime, formatMoney } from "@/shared/lib";
import { Badge, FormError, SectionHeader } from "@/shared/ui";
import { VEHICLE_TEXT } from "@/entities/vehicle/constants/vehicle.constant";
import { useVehicleRequests } from "@/entities/vehicle/hooks/useVehicle";

interface IVehicleRequestListProps {
  vehicleId: string;
}

export const VehicleRequestList = ({ vehicleId }: IVehicleRequestListProps) => {
  const { data, isPending, isError } = useVehicleRequests(vehicleId);

  if (isPending) return <div className="h-24 animate-pulse rounded-xl bg-gray-100" />;
  if (isError) return <FormError message={VEHICLE_TEXT.loadRequestsFailed} />;

  return (
    <section className="flex flex-col gap-2">
      <SectionHeader
        title={VEHICLE_TEXT.requestsTitle}
        description={
          data.total > data.requests.length
            ? `${VEHICLE_TEXT.requestsShown} ${data.requests.length} ${VEHICLE_TEXT.requestsOf} ${data.total}`
            : VEHICLE_TEXT.requestsDescription
        }
      />
      {data.requests.length === 0 && <p className="text-sm text-gray-500">{VEHICLE_TEXT.noRequests}</p>}
      <ul className="flex flex-col divide-y divide-gray-100">
        {data.requests.map((request) => (
          <li key={request.id} className="flex flex-wrap items-center justify-between gap-2 py-2 text-sm">
            <span>
              <span className="font-medium text-ink">{request.code}</span>{" "}
              <span className="text-gray-500">
                {request.ownerName}, {VEHICLE_TEXT.spot} {request.spotNumber}, {formatDateTime(request.startTime)}
              </span>
            </span>
            <span className="flex items-center gap-2">
              <span className="tabular-nums">{formatMoney(request.cost)}</span>
              <Badge>{request.statusTitle}</Badge>
              {request.archived && <Badge>{VEHICLE_TEXT.archived}</Badge>}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
};
