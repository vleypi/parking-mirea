import { OWNER_BULK_ACTIONS, type OwnerBulkAction } from "@/entities/owner";

export const BULK_OWNERS_TEXT = {
  action: "Действие",
  status: "Новый статус",
  tag: "Тег",
  apply: "Применить",
  resultTitle: "Результат массовой операции",
  done: "Готово",
} as const;

export const BULK_ACTION_OPTIONS: readonly { value: OwnerBulkAction; label: string }[] = [
  { value: OWNER_BULK_ACTIONS.archive, label: "В архив" },
  { value: OWNER_BULK_ACTIONS.restore, label: "Восстановить" },
  { value: OWNER_BULK_ACTIONS.changeStatus, label: "Сменить статус" },
  { value: OWNER_BULK_ACTIONS.addTag, label: "Добавить тег" },
  { value: OWNER_BULK_ACTIONS.removeTag, label: "Убрать тег" },
];

export const TAG_BULK_ACTIONS: readonly OwnerBulkAction[] = [OWNER_BULK_ACTIONS.addTag, OWNER_BULK_ACTIONS.removeTag];
