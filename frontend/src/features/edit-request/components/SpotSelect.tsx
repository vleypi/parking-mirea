"use client";

import type { ComponentProps } from "react";
import { formatMoney } from "@/shared/lib";
import { SelectField } from "@/shared/ui";
import type { IRequestSpot } from "@/entities/request";
import { useFreeSpots } from "@/entities/spot";
import { EDIT_REQUEST_TEXT } from "@/features/edit-request/constants/edit-request.constant";
import { toPeriod } from "@/features/edit-request/utils/request-form.util";

interface ISpotSelectProps extends ComponentProps<"select"> {
  from: string;
  to: string;
  current?: IRequestSpot;
  error?: string;
}

const toLabel = (number: number, typeTitle: string, hourlyRate: number) => `№ ${number} · ${typeTitle} · ${formatMoney(hourlyRate)}/ч`;

/** Места, свободные на весь период (GET /spots/free); текущее место заявки всегда в списке. */
export const SpotSelect = ({ from, to, current, error, ...props }: ISpotSelectProps) => {
  const period = toPeriod(from, to);
  const { data } = useFreeSpots(period.from, period.to);
  const options = (data ?? []).map((spot) => ({ value: spot.id, label: toLabel(spot.spotNumber, spot.spotType.title, spot.hourlyRate) }));
  if (current && !options.some(({ value }) => value === current.id)) {
    options.unshift({ value: current.id, label: toLabel(current.number, current.typeTitle, current.hourlyRate) });
  }
  const placeholder = !period.from ? EDIT_REQUEST_TEXT.choosePeriodFirst : data && options.length === 0 ? EDIT_REQUEST_TEXT.noFreeSpots : EDIT_REQUEST_TEXT.chooseSpot;

  return (
    <SelectField
      label={EDIT_REQUEST_TEXT.spot}
      hint={EDIT_REQUEST_TEXT.freeSpotsHint}
      options={current ? options : [{ value: "", label: placeholder }, ...options]}
      error={error}
      disabled={!period.from && !current}
      {...props}
    />
  );
};
