import type { ReactNode } from "react";
import { formatDateTime } from "@/shared/lib";
import { Badge } from "@/shared/ui";
import { VEHICLE_ACTIVITY_TONES, VEHICLE_STATUS_TONES, VEHICLE_TEXT } from "@/entities/vehicle/constants/vehicle.constant";
import type { IVehicle } from "@/entities/vehicle/interfaces/vehicle.interface";

interface IVehicleDetailsProps {
  vehicle: IVehicle;
}

interface IDetailProps {
  label: string;
  children: ReactNode;
}

const Detail = ({ label, children }: IDetailProps) => (
  <div>
    <dt className="text-xs text-gray-500">{label}</dt>
    <dd className="text-sm text-ink">{children}</dd>
  </div>
);

export const VehicleDetails = ({ vehicle }: IVehicleDetailsProps) => (
  <dl className="grid gap-4 rounded-xl bg-canvas p-4 sm:grid-cols-3">
    <Detail label={VEHICLE_TEXT.licensePlate}>
      {vehicle.licensePlate} <span className="text-gray-500">({VEHICLE_TEXT.region} {vehicle.region})</span>
    </Detail>
    <Detail label={VEHICLE_TEXT.vehicle}>
      {vehicle.brand} {vehicle.model}
    </Detail>
    <Detail label={VEHICLE_TEXT.owner}>
      {vehicle.owner.name}, {vehicle.owner.phone}
      {vehicle.owner.archived && <span className="text-gray-500"> ({VEHICLE_TEXT.ownerArchived})</span>}
    </Detail>
    <Detail label={VEHICLE_TEXT.status}>
      <span className="flex flex-wrap gap-1">
        <Badge tone={VEHICLE_STATUS_TONES[vehicle.status.code]}>{vehicle.status.title}</Badge>
        {vehicle.archived && <Badge>{VEHICLE_TEXT.archived}</Badge>}
      </span>
    </Detail>
    <Detail label={VEHICLE_TEXT.activity}>
      <span className="flex items-center gap-2" title={VEHICLE_TEXT.activityHint}>
        <Badge tone={VEHICLE_ACTIVITY_TONES[vehicle.activity.code]}>{vehicle.activity.title}</Badge>
        <span className="text-xs text-gray-500">
          {VEHICLE_TEXT.activityHint}: {vehicle.recentRequests}
        </span>
      </span>
    </Detail>
    <Detail label={VEHICLE_TEXT.year}>{vehicle.releaseYear ?? VEHICLE_TEXT.notSet}</Detail>
    <Detail label={VEHICLE_TEXT.color}>{vehicle.color ?? VEHICLE_TEXT.notSet}</Detail>
    <Detail label={VEHICLE_TEXT.electric}>{vehicle.electric ? VEHICLE_TEXT.yes : VEHICLE_TEXT.no}</Detail>
    <Detail label={VEHICLE_TEXT.tags}>{vehicle.tags.length > 0 ? vehicle.tags.join(", ") : VEHICLE_TEXT.noTags}</Detail>
    <Detail label={VEHICLE_TEXT.createdAt}>{formatDateTime(vehicle.createdAt)}</Detail>
    <Detail label={VEHICLE_TEXT.updatedAt}>{formatDateTime(vehicle.updatedAt)}</Detail>
  </dl>
);
