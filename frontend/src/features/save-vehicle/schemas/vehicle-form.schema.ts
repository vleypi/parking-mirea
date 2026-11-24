import { z } from "zod";
import { MIN_RELEASE_YEAR, VEHICLE_STATUSES } from "@/entities/vehicle";
import {
  LICENSE_PLATE_PATTERN,
  SAVE_VEHICLE_TEXT,
  VEHICLE_COLOR_MAX_LENGTH,
  VEHICLE_TITLE_MAX_LENGTH,
} from "@/features/save-vehicle/constants/save-vehicle.constant";

const isReleaseYear = (value: string) => {
  if (value.trim() === "") return true;
  const year = Number(value);
  return Number.isInteger(year) && year >= MIN_RELEASE_YEAR && year <= new Date().getFullYear();
};

export const vehicleFormSchema = z.object({
  ownerId: z.string().min(1, SAVE_VEHICLE_TEXT.ownerRequired),
  licensePlate: z
    .string()
    .transform((value) => value.replace(/\s+/g, ""))
    .refine((value) => LICENSE_PLATE_PATTERN.test(value), SAVE_VEHICLE_TEXT.licensePlateFormat),
  brand: z.string().trim().min(1, SAVE_VEHICLE_TEXT.required).max(VEHICLE_TITLE_MAX_LENGTH, SAVE_VEHICLE_TEXT.tooLong),
  model: z.string().trim().min(1, SAVE_VEHICLE_TEXT.required).max(VEHICLE_TITLE_MAX_LENGTH, SAVE_VEHICLE_TEXT.tooLong),
  color: z.string().trim().max(VEHICLE_COLOR_MAX_LENGTH, SAVE_VEHICLE_TEXT.tooLong),
  releaseYear: z.string().refine(isReleaseYear, SAVE_VEHICLE_TEXT.yearRange),
  status: z.enum(VEHICLE_STATUSES),
  electric: z.boolean(),
  tags: z.string(),
});

export type VehicleFormInput = z.input<typeof vehicleFormSchema>;
export type VehicleFormValues = z.output<typeof vehicleFormSchema>;
