import { z } from "zod";
import { REQUEST_STATUSES } from "@/entities/request";
import { DESCRIPTION_MAX_LENGTH, EDIT_REQUEST_TEXT } from "@/features/edit-request/constants/edit-request.constant";

export const requestFormSchema = z
  .object({
    vehicleId: z.string().min(1, EDIT_REQUEST_TEXT.vehicleRequired),
    spotId: z.string().min(1, EDIT_REQUEST_TEXT.spotRequired),
    status: z.enum(REQUEST_STATUSES),
    startTime: z.string().min(1, EDIT_REQUEST_TEXT.required),
    endTime: z.string().min(1, EDIT_REQUEST_TEXT.required),
    paid: z.boolean(),
    tags: z.string(),
    description: z.string().max(DESCRIPTION_MAX_LENGTH, EDIT_REQUEST_TEXT.tooLong),
  })
  .refine(({ startTime, endTime }) => endTime > startTime, { message: EDIT_REQUEST_TEXT.periodOrder, path: ["endTime"] });

export type RequestFormValues = z.infer<typeof requestFormSchema>;
