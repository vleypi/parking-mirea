export const CHANGE_PASSWORD_MODES = {
  temporary: "temporary",
  profile: "profile",
} as const;

export type ChangePasswordMode = (typeof CHANGE_PASSWORD_MODES)[keyof typeof CHANGE_PASSWORD_MODES];

export const CHANGE_PASSWORD_MODE_TEXT: Record<ChangePasswordMode, { title: string; description: string; currentPassword: string }> = {
  temporary: {
    title: "Смените временный пароль",
    description: "Администратор выдал вам временный пароль. Придумайте свой, чтобы начать работу",
    currentPassword: "Временный пароль",
  },
  profile: {
    title: "Смена пароля",
    description: "После смены пароля остальные ваши сессии завершатся",
    currentPassword: "Текущий пароль",
  },
};

export const CHANGE_PASSWORD_TEXT = {
  menuItem: "Сменить пароль",
  newPassword: "Новый пароль",
  newPasswordHint: "Не короче 10 символов, хотя бы одна буква и одна цифра",
  confirmPassword: "Повторите новый пароль",
  mismatch: "Пароли не совпадают",
  sameAsCurrent: "Новый пароль должен отличаться от текущего",
  submit: "Сохранить пароль",
  cancel: "Отмена",
} as const;
