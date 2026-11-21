export const OWNER_CARD_TEXT = {
  loadFailed: "Не удалось загрузить владельца",
} as const;

export const ownerNotesPath = (ownerId: string): string => `/owners/${ownerId}/notes`;
