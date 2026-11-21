export const REQUEST_CARD_TEXT = {
  loadFailed: "Не удалось загрузить заявку",
} as const;

export const requestNotesPath = (requestId: string): string => `/requests/${requestId}/notes`;
