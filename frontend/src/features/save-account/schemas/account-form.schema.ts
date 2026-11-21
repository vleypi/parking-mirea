import { z } from "zod";
import { ROLES } from "@/shared/config";
import { loginSchema } from "@/shared/lib";
import { DISPLAY_NAME_MAX_LENGTH, SAVE_ACCOUNT_TEXT } from "@/features/save-account/constants/save-account.constant";

export const accountFormSchema = z.object({
  login: loginSchema,
  displayName: z.string().trim().min(1, SAVE_ACCOUNT_TEXT.required).max(DISPLAY_NAME_MAX_LENGTH, SAVE_ACCOUNT_TEXT.tooLong),
  role: z.enum(ROLES),
  enabled: z.boolean(),
});

export type AccountFormValues = z.infer<typeof accountFormSchema>;
