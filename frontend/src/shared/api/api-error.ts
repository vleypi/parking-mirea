import { isAxiosError } from "axios";
import { API_ERROR_TEXT } from "@/shared/api/api.constant";

export type FieldErrors = Record<string, string[]>;

interface IFieldError {
  field: string;
  message: string;
}

interface IApiErrorBody {
  message?: string;
  detail?: string;
  errors?: FieldErrors;
  fieldErrors?: IFieldError[];
}

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: FieldErrors;

  constructor(message: string, status: number, fieldErrors: FieldErrors = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

const groupFieldErrors = (fieldErrors: IFieldError[] = []): FieldErrors =>
  fieldErrors.reduce<FieldErrors>((grouped, { field, message }) => ({ ...grouped, [field]: [...(grouped[field] ?? []), message] }), {});

export const toApiError = (error: unknown): ApiError => {
  if (error instanceof ApiError) return error;
  if (!isAxiosError<IApiErrorBody>(error)) return new ApiError(API_ERROR_TEXT.unknown, 0);
  if (!error.response) return new ApiError(API_ERROR_TEXT.unavailable, 0);

  const { status, data } = error.response;
  return new ApiError(data?.message ?? data?.detail ?? API_ERROR_TEXT.unknown, status, data?.errors ?? groupFieldErrors(data?.fieldErrors));
};
