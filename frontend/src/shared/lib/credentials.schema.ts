import { z } from "zod";
import { CREDENTIALS, CREDENTIALS_TEXT } from "@/shared/config";

export const passwordSchema = z
  .string()
  .min(CREDENTIALS.passwordMinLength, CREDENTIALS_TEXT.passwordTooShort)
  .max(CREDENTIALS.passwordMaxLength, CREDENTIALS_TEXT.tooLong)
  .regex(CREDENTIALS.passwordLetterPattern, CREDENTIALS_TEXT.passwordNeedsLetter)
  .regex(CREDENTIALS.passwordDigitPattern, CREDENTIALS_TEXT.passwordNeedsDigit);

export const loginSchema = z
  .string()
  .trim()
  .toLowerCase()
  .min(1, CREDENTIALS_TEXT.loginRequired)
  .max(CREDENTIALS.loginMaxLength, CREDENTIALS_TEXT.tooLong)
  .regex(CREDENTIALS.loginPattern, CREDENTIALS_TEXT.loginFormat);
