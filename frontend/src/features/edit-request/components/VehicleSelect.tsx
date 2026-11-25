"use client";

import type { ComponentProps } from "react";
import { SORT_ORDERS } from "@/shared/config";
import { SelectField } from "@/shared/ui";
import type { IRequestVehicle } from "@/entities/request";
import { useVehiclePage, VEHICLE_SORT_FIELDS, VEHICLE_STATUSES } from "@/entities/vehicle";
import { EDIT_REQUEST_TEXT, OWNER_VEHICLES_LIMIT } from "@/features/edit-request/constants/edit-request.constant";

interface IVehicleSelectProps extends ComponentProps<"select"> {
  ownerId: string;
  current?: IRequestVehicle;
  error?: string;
}

const toLabel = ({ licensePlate, brand, model }: IRequestVehicle) => `${licensePlate} · ${brand} ${model}`;

/** Допущенные автомобили выбранного владельца; текущий автомобиль заявки всегда в списке. */
export const VehicleSelect = ({ ownerId, current, error, ...props }: IVehicleSelectProps) => {
  const { data } = useVehiclePage(
    { ownerId, status: VEHICLE_STATUSES.active, size: OWNER_VEHICLES_LIMIT, sort: VEHICLE_SORT_FIELDS.licensePlate, direction: SORT_ORDERS.asc },
    Boolean(ownerId),
  );
  const options = (data?.content ?? []).map((vehicle) => ({ value: vehicle.id, label: toLabel(vehicle) }));
  if (current && !options.some(({ value }) => value === current.id)) {
    options.unshift({ value: current.id, label: toLabel(current) });
  }
  const placeholder = !ownerId ? EDIT_REQUEST_TEXT.chooseOwnerFirst : data && options.length === 0 ? EDIT_REQUEST_TEXT.noVehicles : EDIT_REQUEST_TEXT.chooseVehicle;

  return (
    <SelectField
      label={EDIT_REQUEST_TEXT.vehicle}
      options={current ? options : [{ value: "", label: placeholder }, ...options]}
      error={error}
      disabled={!ownerId}
      {...props}
    />
  );
};
