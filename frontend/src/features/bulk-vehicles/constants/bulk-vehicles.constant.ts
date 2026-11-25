import { VEHICLE_BULK_ACTIONS, type VehicleBulkAction } from "@/entities/vehicle";

export const BULK_VEHICLES_TEXT = {
  action: "Действие",
  status: "Новый статус",
  tag: "Тег",
  apply: "Применить",
  resultTitle: "Результат массовой операции",
  done: "Готово",
} as const;

export const BULK_ACTION_OPTIONS: readonly { value: VehicleBulkAction; label: string }[] = [
  { value: VEHICLE_BULK_ACTIONS.archive, label: "В архив" },
  { value: VEHICLE_BULK_ACTIONS.restore, label: "Восстановить" },
  { value: VEHICLE_BULK_ACTIONS.changeStatus, label: "Сменить статус" },
  { value: VEHICLE_BULK_ACTIONS.addTag, label: "Добавить тег" },
  { value: VEHICLE_BULK_ACTIONS.removeTag, label: "Убрать тег" },
];

export const TAG_BULK_ACTIONS: readonly VehicleBulkAction[] = [VEHICLE_BULK_ACTIONS.addTag, VEHICLE_BULK_ACTIONS.removeTag];
