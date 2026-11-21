import { REQUEST_BULK_ACTIONS, type RequestBulkAction } from "@/entities/request";

export const BULK_REQUESTS_TEXT = {
  action: "Действие",
  status: "Новый статус",
  tag: "Тег",
  apply: "Применить",
  resultTitle: "Результат массовой операции",
  done: "Готово",
  tagRequired: "Укажите тег",
} as const;

export const BULK_ACTION_OPTIONS: readonly { value: RequestBulkAction; label: string }[] = [
  { value: REQUEST_BULK_ACTIONS.archive, label: "В архив" },
  { value: REQUEST_BULK_ACTIONS.restore, label: "Восстановить" },
  { value: REQUEST_BULK_ACTIONS.changeStatus, label: "Сменить статус" },
  { value: REQUEST_BULK_ACTIONS.addTag, label: "Добавить тег" },
  { value: REQUEST_BULK_ACTIONS.removeTag, label: "Убрать тег" },
];

export const TAG_BULK_ACTIONS: readonly RequestBulkAction[] = [REQUEST_BULK_ACTIONS.addTag, REQUEST_BULK_ACTIONS.removeTag];
