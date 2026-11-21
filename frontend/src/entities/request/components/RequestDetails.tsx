import type { ReactNode } from "react";
import { formatDateTime, formatMoney } from "@/shared/lib";
import { Badge } from "@/shared/ui";
import { REQUEST_PRIORITY_LABELS, REQUEST_PRIORITY_TONES, REQUEST_STATUS_TONES, REQUEST_TEXT } from "@/entities/request/constants/request.constant";
import type { IRequest } from "@/entities/request/interfaces/request.interface";

interface IRequestDetailsProps {
  request: IRequest;
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

export const RequestDetails = ({ request }: IRequestDetailsProps) => (
  <dl className="grid gap-4 rounded-xl bg-canvas p-4 sm:grid-cols-3">
    <Detail label={REQUEST_TEXT.owner}>
      {request.owner.name}, {request.owner.phone}
    </Detail>
    <Detail label={REQUEST_TEXT.vehicle}>
      {request.vehicle.licensePlate}, {request.vehicle.brand} {request.vehicle.model}
    </Detail>
    <Detail label={REQUEST_TEXT.spot}>
      {REQUEST_TEXT.spotNumber} {request.spot.number}, {request.spot.typeTitle}, {formatMoney(request.spot.hourlyRate)} {REQUEST_TEXT.hourlyRate}
    </Detail>
    <Detail label={REQUEST_TEXT.status}>
      <span className="flex flex-wrap gap-1">
        <Badge tone={REQUEST_STATUS_TONES[request.status.code]}>{request.status.title}</Badge>
        {request.archived && <Badge>{REQUEST_TEXT.archived}</Badge>}
      </span>
    </Detail>
    <Detail label={REQUEST_TEXT.priority}>
      <Badge tone={REQUEST_PRIORITY_TONES[request.priority]}>{REQUEST_PRIORITY_LABELS[request.priority]}</Badge>
    </Detail>
    <Detail label={REQUEST_TEXT.cost}>
      {formatMoney(request.cost)}, {request.paid ? REQUEST_TEXT.paid : REQUEST_TEXT.unpaid}
    </Detail>
    <Detail label={REQUEST_TEXT.createdAt}>{formatDateTime(request.createdAt)}</Detail>
    <Detail label={REQUEST_TEXT.updatedAt}>{formatDateTime(request.updatedAt)}</Detail>
  </dl>
);
