export const LOGO_TEXT = {
  mark: "P",
  label: "Парковка",
} as const;

export const CARD_CLASS_NAME = "rounded-xl border border-gray-200 bg-white";

export const TOOLBAR_CONTROL_CLASS_NAME =
  "h-8 rounded-xl border border-gray-300 bg-white text-sm text-ink transition placeholder:text-gray-500 hover:border-gray-400 focus:border-brand focus:ring-2 focus:ring-brand/15 focus:outline-none";

export const POPOVER_PANEL_CLASS_NAME =
  "-translate-y-1 rounded-2xl border border-gray-100 bg-white opacity-0 shadow-lg transition-all transition-discrete duration-150 ease-out-expo open:translate-y-0 open:opacity-100 starting:open:-translate-y-1 starting:open:opacity-0";

export const MENU_ITEM_CLASS_NAME = "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm text-ink transition hover:bg-gray-100";

export const UI_TEXT = {
  cancel: "Отмена",
  close: "Закрыть",
  selected: "Выбрано",
  clearSelection: "Снять выделение",
  selectAll: "Выбрать все на странице",
  selectRow: "Выбрать строку",
  bulkSucceeded: "Выполнено",
  bulkFailed: "Не выполнено",
} as const;
