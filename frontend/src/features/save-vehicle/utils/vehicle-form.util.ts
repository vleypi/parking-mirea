import { formatTags, parseTags } from "@/shared/lib";
import { type IVehicle, type IVehiclePayload, VEHICLE_STATUSES } from "@/entities/vehicle";
import type { VehicleFormInput, VehicleFormValues } from "@/features/save-vehicle/schemas/vehicle-form.schema";

export const toVehicleFormValues = (vehicle: IVehicle | null): VehicleFormInput =>
  vehicle
    ? {
        ownerId: vehicle.owner.id,
        licensePlate: vehicle.licensePlate,
        brand: vehicle.brand,
        model: vehicle.model,
        color: vehicle.color ?? "",
        releaseYear: vehicle.releaseYear ? String(vehicle.releaseYear) : "",
        status: vehicle.status.code,
        electric: vehicle.electric,
        tags: formatTags(vehicle.tags),
      }
    : {
        ownerId: "",
        licensePlate: "",
        brand: "",
        model: "",
        color: "",
        releaseYear: "",
        status: VEHICLE_STATUSES.active,
        electric: false,
        tags: "",
      };

export const toVehiclePayload = (values: VehicleFormValues): IVehiclePayload => ({
  ownerId: values.ownerId,
  licensePlate: values.licensePlate.toUpperCase(),
  brand: values.brand,
  model: values.model,
  color: values.color || null,
  releaseYear: values.releaseYear.trim() ? Number(values.releaseYear) : null,
  electric: values.electric,
  status: values.status,
  tags: parseTags(values.tags),
});
