import { formatTags, parseTags } from "@/shared/lib";
import { type ISpot, type ISpotPayload, SPOT_STATUSES } from "@/entities/spot";
import type { SpotFormValues } from "@/features/save-spot/schemas/spot-form.schema";

export const toSpotFormValues = (spot: ISpot | null): SpotFormValues =>
  spot
    ? {
        spotNumber: String(spot.spotNumber),
        spotTypeId: String(spot.spotType.id),
        hourlyRate: String(spot.hourlyRate),
        zone: spot.zone ?? "",
        description: spot.description ?? "",
        status: spot.status.code,
        covered: spot.covered,
        tags: formatTags(spot.tags),
      }
    : {
        spotNumber: "",
        spotTypeId: "",
        hourlyRate: "",
        zone: "",
        description: "",
        status: SPOT_STATUSES.available,
        covered: false,
        tags: "",
      };

export const toSpotPayload = (values: SpotFormValues): ISpotPayload => ({
  spotNumber: Number(values.spotNumber),
  spotTypeId: Number(values.spotTypeId),
  hourlyRate: Number(values.hourlyRate.replace(",", ".")),
  zone: values.zone || null,
  description: values.description || null,
  status: values.status,
  covered: values.covered,
  tags: parseTags(values.tags),
});
