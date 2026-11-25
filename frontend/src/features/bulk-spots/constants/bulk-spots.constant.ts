import { SPOT_BULK_ACTIONS, type SpotBulkAction } from "@/entities/spot";

export const BULK_SPOTS_TEXT = {
  action: "Действие",
  status: "Новый статус",
  tag: "Тег",
  apply: "Применить",
  resultTitle: "Результат массовой операции",
  done: "Готово",
} as const;

export const BULK_ACTION_OPTIONS: readonly { value: SpotBulkAction; label: string }[] = [
  { value: SPOT_BULK_ACTIONS.archive, label: "В архив" },
  { value: SPOT_BULK_ACTIONS.restore, label: "Восстановить" },
  { value: SPOT_BULK_ACTIONS.changeStatus, label: "Сменить статус" },
  { value: SPOT_BULK_ACTIONS.addTag, label: "Добавить тег" },
  { value: SPOT_BULK_ACTIONS.removeTag, label: "Убрать тег" },
];

export const TAG_BULK_ACTIONS: readonly SpotBulkAction[] = [SPOT_BULK_ACTIONS.addTag, SPOT_BULK_ACTIONS.removeTag];
