import { formatTags, parseTags, toDateTimeInput } from "@/shared/lib";
import type { IRequest, IRequestUpdatePayload } from "@/entities/request";
import type { RequestFormValues } from "@/features/edit-request/schemas/request-form.schema";

export const toRequestFormValues = (request: IRequest): RequestFormValues => ({
  status: request.status.code,
  startTime: toDateTimeInput(request.startTime),
  endTime: toDateTimeInput(request.endTime),
  paid: request.paid,
  tags: formatTags(request.tags),
  description: request.description ?? "",
});

export const toUpdatePayload = (request: IRequest, values: RequestFormValues): IRequestUpdatePayload => ({
  vehicleId: request.vehicle.id,
  spotId: request.spot.id,
  startTime: values.startTime,
  endTime: values.endTime,
  status: values.status,
  description: values.description.trim(),
  paid: values.paid,
  tags: parseTags(values.tags),
  version: request.version,
});
