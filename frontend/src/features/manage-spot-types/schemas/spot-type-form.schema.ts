import { z } from "zod";
import {
  MANAGE_SPOT_TYPES_TEXT,
  MAX_SORT_ORDER,
  SPOT_TYPE_CODE_PATTERN,
  SPOT_TYPE_TITLE_MAX_LENGTH,
} from "@/features/manage-spot-types/constants/manage-spot-types.constant";

const isSortOrder = (value: string) => value.trim() === "" || (/^\d+$/.test(value.trim()) && Number(value) <= MAX_SORT_ORDER);

export const spotTypeFormSchema = z.object({
  code: z.string().trim().regex(SPOT_TYPE_CODE_PATTERN, MANAGE_SPOT_TYPES_TEXT.codeFormat),
  title: z.string().trim().min(1, MANAGE_SPOT_TYPES_TEXT.required).max(SPOT_TYPE_TITLE_MAX_LENGTH, MANAGE_SPOT_TYPES_TEXT.tooLong),
  sortOrder: z.string().refine(isSortOrder, MANAGE_SPOT_TYPES_TEXT.sortOrderFormat),
});

export type SpotTypeFormValues = z.infer<typeof spotTypeFormSchema>;
