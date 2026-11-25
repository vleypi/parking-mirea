import type { ReactNode } from "react";
import { formatDateTime, formatMoney } from "@/shared/lib";
import { Badge } from "@/shared/ui";
import { SPOT_AVAILABILITY_TONES, SPOT_STATUS_TONES, SPOT_TEXT } from "@/entities/spot/constants/spot.constant";
import type { ISpot } from "@/entities/spot/interfaces/spot.interface";

interface ISpotDetailsProps {
  spot: ISpot;
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

export const SpotDetails = ({ spot }: ISpotDetailsProps) => (
  <dl className="grid gap-4 rounded-xl bg-canvas p-4 sm:grid-cols-3">
    <Detail label={SPOT_TEXT.spot}>№ {spot.spotNumber}</Detail>
    <Detail label={SPOT_TEXT.type}>
      {spot.spotType.title}
      {!spot.spotType.active && <span className="text-gray-500"> ({SPOT_TEXT.typeDisabled})</span>}
    </Detail>
    <Detail label={SPOT_TEXT.rate}>
      <span className="tabular-nums">{formatMoney(spot.hourlyRate)}</span> {SPOT_TEXT.perHour}
    </Detail>
    <Detail label={SPOT_TEXT.status}>
      <span className="flex flex-wrap gap-1">
        <Badge tone={SPOT_STATUS_TONES[spot.status.code]}>{spot.status.title}</Badge>
        {spot.archived && <Badge>{SPOT_TEXT.archived}</Badge>}
      </span>
    </Detail>
    <Detail label={SPOT_TEXT.availability}>
      <Badge tone={SPOT_AVAILABILITY_TONES[spot.availability.code]}>{spot.availability.title}</Badge>
    </Detail>
    <Detail label={SPOT_TEXT.covered}>{spot.covered ? SPOT_TEXT.yes : SPOT_TEXT.no}</Detail>
    <Detail label={SPOT_TEXT.zone}>{spot.zone ?? SPOT_TEXT.notSet}</Detail>
    <Detail label={SPOT_TEXT.tags}>{spot.tags.length > 0 ? spot.tags.join(", ") : SPOT_TEXT.noTags}</Detail>
    <Detail label={SPOT_TEXT.createdAt}>{formatDateTime(spot.createdAt)}</Detail>
    {spot.description && (
      <div className="sm:col-span-3">
        <Detail label={SPOT_TEXT.description}>{spot.description}</Detail>
      </div>
    )}
    <Detail label={SPOT_TEXT.updatedAt}>{formatDateTime(spot.updatedAt)}</Detail>
  </dl>
);
