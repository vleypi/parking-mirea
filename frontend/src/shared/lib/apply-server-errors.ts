import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import { toApiError } from "@/shared/api/api-error";

export const SERVER_ERROR_FIELD = "root.server";

export const applyServerErrors = <TValues extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<TValues>,
  fieldAliases: Partial<Record<string, Path<TValues>>> = {},
): void => {
  const { message, fieldErrors } = toApiError(error);
  const fields = Object.entries(fieldErrors);

  fields.forEach(([field, messages]) => setError(fieldAliases[field] ?? (field as Path<TValues>), { message: messages.join(". ") }));
  if (fields.length === 0) setError(SERVER_ERROR_FIELD, { message });
};
