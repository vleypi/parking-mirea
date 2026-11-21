export const API_BASE_PATH = "/api";
export const API_REQUEST_TIMEOUT_MS = 10_000;

export const HTTP_STATUS = {
  unauthorized: 401,
  forbidden: 403,
  notFound: 404,
} as const;

export const API_ERROR_TEXT = {
  unavailable: "Сервер недоступен. Попробуйте позже",
  unknown: "Что-то пошло не так",
} as const;
