const DATE_TIME_INPUT_LENGTH = "yyyy-MM-ddTHH:mm".length;

export const toDateTimeInput = (value: string): string => value.slice(0, DATE_TIME_INPUT_LENGTH);
