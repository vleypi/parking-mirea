export const VEHICLE_CARD_TEXT = {
  loadFailed: "Не удалось загрузить автомобиль",
} as const;

export const vehicleNotesPath = (vehicleId: string): string => `/vehicles/${vehicleId}/notes`;
