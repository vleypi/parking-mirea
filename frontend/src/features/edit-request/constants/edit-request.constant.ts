export const EDIT_REQUEST_TEXT = {
  status: "Статус",
  startTime: "Начало",
  endTime: "Окончание",
  paid: "Оплачена",
  tags: "Теги",
  tagsHint: "Через запятую",
  description: "Описание",
  periodOrder: "Окончание должно быть позже начала",
  required: "Заполните поле",
  tooLong: "Слишком длинное значение",
  owner: "Владелец",
  ownerRequired: "Выберите владельца из списка",
  vehicle: "Автомобиль",
  vehicleRequired: "Выберите автомобиль",
  chooseVehicle: "Выберите автомобиль",
  chooseOwnerFirst: "Сначала выберите владельца",
  noVehicles: "У владельца нет допущенных автомобилей",
  spot: "Место",
  spotRequired: "Выберите место",
  chooseSpot: "Выберите место",
  choosePeriodFirst: "Сначала укажите период",
  noFreeSpots: "Свободных мест на этот период нет",
  freeSpotsHint: "Показаны места, свободные на весь период",
  estimatedCost: "Предварительная стоимость",
  create: "Оформить заявку",
  cancel: "Отмена",
  save: "Сохранить изменения",
  readOnly: "Заявку нельзя изменить: она в архиве, завершена или отменена",
} as const;

export const DESCRIPTION_MAX_LENGTH = 500;

/** Машин владельца в выборе: допущенные, не из архива. */
export const OWNER_VEHICLES_LIMIT = 100;
