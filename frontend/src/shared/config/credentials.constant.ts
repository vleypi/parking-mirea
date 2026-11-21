export const CREDENTIALS = {
  loginMaxLength: 50,
  loginPattern: /^[a-z0-9._-]+$/,
  passwordMinLength: 10,
  passwordMaxLength: 72,
  passwordLetterPattern: /\p{L}/u,
  passwordDigitPattern: /\d/,
} as const;

export const CREDENTIALS_TEXT = {
  loginRequired: "Введите логин",
  loginFormat: "Только латинские буквы, цифры, точка, дефис и подчеркивание",
  passwordRequired: "Введите пароль",
  passwordTooShort: `Минимум ${CREDENTIALS.passwordMinLength} символов`,
  passwordNeedsLetter: "Нужна хотя бы одна буква",
  passwordNeedsDigit: "Нужна хотя бы одна цифра",
  tooLong: "Слишком длинное значение",
} as const;
