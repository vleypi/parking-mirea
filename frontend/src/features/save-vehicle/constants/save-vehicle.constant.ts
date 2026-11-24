import { LICENSE_PLATE_EXAMPLE, MIN_RELEASE_YEAR } from "@/entities/vehicle";

export const SAVE_VEHICLE_TEXT = {
  owner: "Владелец",
  ownerSearch: "Имя или телефон владельца",
  ownerNotFound: "Никого не нашли",
  ownerChange: "Сменить",
  ownerRequired: "Выберите владельца из списка",
  licensePlate: "Гос. номер",
  licensePlateHint: `Например, ${LICENSE_PLATE_EXAMPLE}; латиница заменится на кириллицу`,
  licensePlateFormat: `Гос. номер в формате ${LICENSE_PLATE_EXAMPLE}: буква, 3 цифры, 2 буквы, регион из 2–3 цифр`,
  brand: "Марка",
  model: "Модель",
  color: "Цвет",
  colorHint: "Необязательно",
  year: "Год выпуска",
  yearHint: `Необязательно, от ${MIN_RELEASE_YEAR}`,
  yearRange: `Год от ${MIN_RELEASE_YEAR} до текущего`,
  status: "Статус",
  tags: "Теги",
  tagsHint: "Через запятую",
  electric: "Электромобиль",
  required: "Заполните поле",
  tooLong: "Слишком длинное значение",
  create: "Добавить автомобиль",
  save: "Сохранить изменения",
  cancel: "Отмена",
  readOnly: "Автомобиль в архиве: чтобы изменить данные, сначала восстановите его",
} as const;

export const VEHICLE_TITLE_MAX_LENGTH = 50;
export const VEHICLE_COLOR_MAX_LENGTH = 30;
export const OWNER_SEARCH_LIMIT = 8;

/** Допускаются латинские двойники букв и пробелы — сервер нормализует номер сам. */
export const LICENSE_PLATE_PATTERN = /^[АВЕКМНОРСТУХABEKMHOPCTYX]\d{3}[АВЕКМНОРСТУХABEKMHOPCTYX]{2}\d{2,3}$/i;
