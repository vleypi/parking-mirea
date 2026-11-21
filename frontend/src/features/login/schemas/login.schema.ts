import { z } from "zod";
import { CREDENTIALS, CREDENTIALS_TEXT } from "@/shared/config";

export const loginFormSchema = z.object({
  login: z.string().trim().min(1, CREDENTIALS_TEXT.loginRequired).max(CREDENTIALS.loginMaxLength, CREDENTIALS_TEXT.tooLong),
  password: z.string().min(1, CREDENTIALS_TEXT.passwordRequired).max(CREDENTIALS.passwordMaxLength, CREDENTIALS_TEXT.tooLong),
});

export type LoginFormValues = z.infer<typeof loginFormSchema>;
