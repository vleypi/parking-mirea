const LOCALE = "ru-RU";

const DATE_TIME_FORMAT = new Intl.DateTimeFormat(LOCALE, { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });

const MONEY_FORMAT = new Intl.NumberFormat(LOCALE, { style: "currency", currency: "RUB", maximumFractionDigits: 2 });

export const formatDateTime = (value: string): string => DATE_TIME_FORMAT.format(new Date(value));

export const formatMoney = (value: number): string => MONEY_FORMAT.format(value);
