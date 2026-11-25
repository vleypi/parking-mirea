import { formatTags, parseTags, toDateTimeInput } from "@/shared/lib";
import type { IRequest, IRequestCreatePayload, IRequestUpdatePayload } from "@/entities/request";
import type { CreateRequestFormValues } from "@/features/edit-request/schemas/create-request-form.schema";
import type { RequestFormValues } from "@/features/edit-request/schemas/request-form.schema";

export const toRequestFormValues = (request: IRequest): RequestFormValues => ({
  vehicleId: request.vehicle.id,
  spotId: request.spot.id,
  status: request.status.code,
  startTime: toDateTimeInput(request.startTime),
  endTime: toDateTimeInput(request.endTime),
  paid: request.paid,
  tags: formatTags(request.tags),
  description: request.description ?? "",
});

export const toUpdatePayload = (request: IRequest, values: RequestFormValues): IRequestUpdatePayload => ({
  vehicleId: values.vehicleId,
  spotId: values.spotId,
  startTime: values.startTime,
  endTime: values.endTime,
  status: values.status,
  description: values.description.trim(),
  paid: values.paid,
  tags: parseTags(values.tags),
  version: request.version,
});

export const EMPTY_CREATE_FORM: CreateRequestFormValues = {
  ownerId: "",
  vehicleId: "",
  spotId: "",
  startTime: "",
  endTime: "",
  paid: false,
  tags: "",
  description: "",
};

export const toCreatePayload = (values: CreateRequestFormValues): IRequestCreatePayload => ({
  ownerId: values.ownerId,
  vehicleId: values.vehicleId,
  spotId: values.spotId,
  startTime: values.startTime,
  endTime: values.endTime,
  description: values.description.trim(),
  paid: values.paid,
  tags: parseTags(values.tags),
});

/** Период для поиска свободных мест: пустой, пока конец не позже начала. */
export const toPeriod = (from: string, to: string) => (from && to > from ? { from, to } : { from: "", to: "" });

/** Та же формула, что на сервере: тариф × минуты / 60 с округлением до копеек. */
export const estimateCost = (hourlyRate: number, startTime: string, endTime: string): number => {
  const minutes = Math.floor((new Date(endTime).getTime() - new Date(startTime).getTime()) / 60000);
  return Math.round((hourlyRate * minutes * 100) / 60) / 100;
};
