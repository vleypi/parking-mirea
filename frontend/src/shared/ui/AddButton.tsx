import { Icon } from "@/shared/ui/Icon";

export interface IAddAction {
  label: string;
  onClick: () => void;
  disabled?: boolean;
}

export const AddButton = ({ label, onClick, disabled = false }: IAddAction) => (
  <button
    type="button"
    onClick={onClick}
    disabled={disabled}
    aria-label={label}
    className="group/add inline-flex size-9 shrink-0 items-center justify-center rounded-full bg-lime text-forest transition hover:scale-110 hover:shadow-md focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:outline-none active:scale-95 disabled:pointer-events-none disabled:opacity-40"
  >
    <Icon name="plus" className="size-4.5 transition-transform duration-300 group-hover/add:rotate-90" />
  </button>
);
