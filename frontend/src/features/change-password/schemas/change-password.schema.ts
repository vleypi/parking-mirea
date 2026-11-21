import { z } from "zod";
import { CREDENTIALS_TEXT } from "@/shared/config";
import { passwordSchema } from "@/shared/lib";
import { CHANGE_PASSWORD_TEXT } from "@/features/change-password/constants/change-password.constant";

export const changePasswordFormSchema = z
  .object({
    currentPassword: z.string().min(1, CREDENTIALS_TEXT.passwordRequired),
    newPassword: passwordSchema,
    confirmPassword: z.string(),
  })
  .refine(({ newPassword, confirmPassword }) => newPassword === confirmPassword, { message: CHANGE_PASSWORD_TEXT.mismatch, path: ["confirmPassword"] })
  .refine(({ currentPassword, newPassword }) => currentPassword !== newPassword, { message: CHANGE_PASSWORD_TEXT.sameAsCurrent, path: ["newPassword"] });

export type ChangePasswordFormValues = z.infer<typeof changePasswordFormSchema>;
