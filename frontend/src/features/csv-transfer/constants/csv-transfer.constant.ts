import type { ImportMode } from "@/shared/api";

export const CSV_TRANSFER_TEXT = {
  export: "Экспорт CSV",
  import: "Импорт CSV",
  importTitle: "Импорт из CSV",
  file: "Файл CSV",
  fileHint: "UTF-8 или Windows-1251, разделитель «;» или «,», до 2 МБ и 5000 строк",
  check: "Проверить файл",
  mode: "Режим",
  apply: "Применить",
  cancel: "Отмена",
  done: "Готово",
  checkAgain: "Выбрать другой файл",
  total: "Строк в файле",
  valid: "Корректных",
  invalid: "С ошибками",
  row: "Строка",
  previewNote: "Файл проверен, в базу пока ничего не записано. Строки с ошибками при применении будут пропущены",
  nothingToApply: "В файле нет корректных строк — исправьте ошибки и загрузите файл снова",
  created: "Создано",
  updated: "Обновлено",
  skipped: "Пропущено",
  applyErrors: "Не записаны",
  exportFailed: "Не удалось выгрузить файл",
} as const;

export const IMPORT_MODE_OPTIONS: readonly { value: ImportMode; label: string }[] = [
  { value: "CREATE_ONLY", label: "Только новые записи, существующие пропустить" },
  { value: "UPSERT", label: "Новые добавить, существующие обновить" },
];
