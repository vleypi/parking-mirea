export const SPOT_CARD_TEXT = {
  loadFailed: "Не удалось загрузить место",
} as const;

export const spotNotesPath = (spotId: string): string => `/spots/${spotId}/notes`;
