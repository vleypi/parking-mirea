import { z } from "zod";
import { MAX_HOURLY_RATE, SPOT_STATUSES } from "@/entities/spot";
import {
  RATE_PATTERN,
  SAVE_SPOT_TEXT,
  SPOT_DESCRIPTION_MAX_LENGTH,
  SPOT_ZONE_MAX_LENGTH,
} from "@/features/save-spot/constants/save-spot.constant";

const isSpotNumber = (value: string) => /^\d+$/.test(value.trim()) && Number(value) > 0;

const isRate = (value: string) => RATE_PATTERN.test(value.trim()) && Number(value.replace(",", ".")) <= MAX_HOURLY_RATE;

export const spotFormSchema = z.object({
  spotNumber: z.string().refine(isSpotNumber, SAVE_SPOT_TEXT.numberFormat),
  spotTypeId: z.string().min(1, SAVE_SPOT_TEXT.typeRequired),
  hourlyRate: z.string().refine(isRate, SAVE_SPOT_TEXT.rateFormat),
  zone: z.string().trim().max(SPOT_ZONE_MAX_LENGTH, SAVE_SPOT_TEXT.tooLong),
  description: z.string().trim().max(SPOT_DESCRIPTION_MAX_LENGTH, SAVE_SPOT_TEXT.tooLong),
  status: z.enum(SPOT_STATUSES),
  covered: z.boolean(),
  tags: z.string(),
});

export type SpotFormValues = z.infer<typeof spotFormSchema>;
