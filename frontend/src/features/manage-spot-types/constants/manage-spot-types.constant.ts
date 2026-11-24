export const MANAGE_SPOT_TYPES_TEXT = {
  code: "Код",
  codeHint: "Латиница, цифры и _, начинается с буквы; например COVERED",
  codeFormat: "Код: от 2 до 20 символов, латиница, цифры и _, начинается с буквы",
  title: "Название",
  sortOrder: "Порядок",
  sortOrderHint: "Необязательно, от 0 до 9999; меньше — выше в списках",
  sortOrderFormat: "Порядок — целое число от 0 до 9999",
  required: "Заполните поле",
  tooLong: "Не длиннее 50 символов",
  create: "Добавить тип",
  save: "Сохранить",
  cancel: "Отмена",
  enable: "Включить",
  disable: "Отключить",
} as const;

export const SPOT_TYPE_CODE_PATTERN = /^[A-Za-z][A-Za-z0-9_]{1,19}$/;
export const SPOT_TYPE_TITLE_MAX_LENGTH = 50;
export const MAX_SORT_ORDER = 9999;
