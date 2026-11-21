import { z } from "zod";
import { OWNER_STATUSES } from "@/entities/owner";
import {
  MAX_DISCOUNT,
  OWNER_EMAIL_MAX_LENGTH,
  OWNER_NAME_MAX_LENGTH,
  OWNER_PHONE_PATTERN,
  SAVE_OWNER_TEXT,
} from "@/features/save-owner/constants/save-owner.constant";

const isDiscount = (value: string) => {
  const number = Number(value.replace(",", "."));
  return value.trim() !== "" && Number.isFinite(number) && number >= 0 && number <= MAX_DISCOUNT;
};

export const ownerFormSchema = z.object({
  name: z.string().trim().min(1, SAVE_OWNER_TEXT.required).max(OWNER_NAME_MAX_LENGTH, SAVE_OWNER_TEXT.tooLong),
  phone: z.string().trim().regex(OWNER_PHONE_PATTERN, SAVE_OWNER_TEXT.phoneFormat),
  email: z
    .string()
    .trim()
    .max(OWNER_EMAIL_MAX_LENGTH, SAVE_OWNER_TEXT.tooLong)
    .refine((value) => value === "" || z.email().safeParse(value).success, SAVE_OWNER_TEXT.emailFormat),
  categoryId: z.string().min(1, SAVE_OWNER_TEXT.required),
  status: z.enum(OWNER_STATUSES),
  discountPercent: z.string().refine(isDiscount, SAVE_OWNER_TEXT.discountRange),
  notificationsEnabled: z.boolean(),
  tags: z.string(),
});

export type OwnerFormValues = z.infer<typeof ownerFormSchema>;
