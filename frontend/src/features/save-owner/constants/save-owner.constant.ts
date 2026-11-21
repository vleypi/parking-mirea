export const SAVE_OWNER_TEXT = {
  name: "Имя",
  phone: "Телефон",
  phoneHint: "+7-XXX-XXX-XX-XX",
  email: "Email",
  emailHint: "Необязательно",
  category: "Категория",
  status: "Статус",
  discount: "Скидка, %",
  discountHint: "От 0 до 50",
  tags: "Теги",
  tagsHint: "Через запятую",
  notifications: "Получает уведомления",
  required: "Заполните поле",
  tooLong: "Слишком длинное значение",
  phoneFormat: "Телефон в формате +7-XXX-XXX-XX-XX",
  emailFormat: "Некорректный email",
  discountRange: "Скидка от 0 до 50 %",
  create: "Добавить владельца",
  save: "Сохранить изменения",
  cancel: "Отмена",
  readOnly: "Владелец в архиве: чтобы изменить данные, сначала восстановите его",
} as const;

export const OWNER_NAME_MAX_LENGTH = 100;
export const OWNER_EMAIL_MAX_LENGTH = 100;
export const OWNER_PHONE_PATTERN = /^\+7-[3489]\d{2}-\d{3}-\d{2}-\d{2}$/;
export const MAX_DISCOUNT = 50;
