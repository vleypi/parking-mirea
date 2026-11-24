import { MAX_HOURLY_RATE } from "@/entities/spot";

export const SAVE_SPOT_TEXT = {
  spotNumber: "Номер места",
  type: "Тип места",
  typePlaceholder: "Выберите тип",
  typeRequired: "Укажите тип места",
  rate: "Тариф, ₽ в час",
  rateHint: `От 0 до ${MAX_HOURLY_RATE}, не больше двух знаков после запятой`,
  rateFormat: `Тариф от 0 до ${MAX_HOURLY_RATE}, не больше двух знаков после запятой`,
  numberFormat: "Номер места должен быть положительным целым числом",
  zone: "Зона",
  zoneHint: "Необязательно, например «A» или «Подземный этаж»",
  description: "Описание",
  status: "Статус",
  covered: "Крытое место",
  tags: "Теги",
  tagsHint: "Через запятую",
  tooLong: "Слишком длинное значение",
  create: "Добавить место",
  save: "Сохранить изменения",
  cancel: "Отмена",
  readOnly: "Место в архиве: чтобы изменить данные, сначала восстановите его",
} as const;

export const SPOT_ZONE_MAX_LENGTH = 20;
export const SPOT_DESCRIPTION_MAX_LENGTH = 500;

/** Число с точкой или запятой и не больше чем двумя знаками после неё. */
export const RATE_PATTERN = /^\d{1,5}([.,]\d{1,2})?$/;
