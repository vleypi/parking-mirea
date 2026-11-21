import type { ReactNode } from "react";
import { formatDateTime } from "@/shared/lib";
import { Badge } from "@/shared/ui";
import { LOYALTY_TONES, OWNER_STATUS_TONES, OWNER_TEXT } from "@/entities/owner/constants/owner.constant";
import type { IOwner } from "@/entities/owner/interfaces/owner.interface";

interface IOwnerDetailsProps {
  owner: IOwner;
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

export const OwnerDetails = ({ owner }: IOwnerDetailsProps) => (
  <dl className="grid gap-4 rounded-xl bg-canvas p-4 sm:grid-cols-3">
    <Detail label={OWNER_TEXT.phone}>{owner.phone}</Detail>
    <Detail label={OWNER_TEXT.email}>{owner.email ?? OWNER_TEXT.noEmail}</Detail>
    <Detail label={OWNER_TEXT.category}>{owner.category.title}</Detail>
    <Detail label={OWNER_TEXT.status}>
      <span className="flex flex-wrap gap-1">
        <Badge tone={OWNER_STATUS_TONES[owner.status.code]}>{owner.status.title}</Badge>
        {owner.archived && <Badge>{OWNER_TEXT.archived}</Badge>}
      </span>
    </Detail>
    <Detail label={OWNER_TEXT.loyalty}>
      <span className="flex items-center gap-2">
        <Badge tone={LOYALTY_TONES[owner.loyalty.code]}>{owner.loyalty.title}</Badge>
        <span className="text-xs text-gray-500">
          {OWNER_TEXT.completedRequests}: {owner.completedRequests}
        </span>
      </span>
    </Detail>
    <Detail label={OWNER_TEXT.discount}>{owner.discountPercent} %</Detail>
    <Detail label={OWNER_TEXT.notifications}>{owner.notificationsEnabled ? OWNER_TEXT.notificationsOn : OWNER_TEXT.notificationsOff}</Detail>
    <Detail label={OWNER_TEXT.tags}>{owner.tags.length > 0 ? owner.tags.join(", ") : OWNER_TEXT.noTags}</Detail>
    <Detail label={OWNER_TEXT.createdAt}>{formatDateTime(owner.createdAt)}</Detail>
    <Detail label={OWNER_TEXT.updatedAt}>{formatDateTime(owner.updatedAt)}</Detail>
  </dl>
);
