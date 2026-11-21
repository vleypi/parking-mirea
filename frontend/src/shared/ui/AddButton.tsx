import { joinClassNames } from "@/shared/lib";
import { Icon } from "@/shared/ui/Icon";

export interface IAddAction {
  label: string;
  onClick: () => void;
  disabled?: boolean;
}

const REVEAL_CLASS_NAME = "group-hover/add:grid-cols-[1fr] group-focus-visible/add:grid-cols-[1fr]";
const LABEL_SHOWN_CLASS_NAME = "group-hover/add:opacity-100 group-focus-visible/add:opacity-100";

export const AddButton = ({ label, onClick, disabled = false }: IAddAction) => (
  <button
    type="button"
    onClick={onClick}
    disabled={disabled}
    aria-label={label}
    className="group/add inline-flex h-9 shrink-0 items-center rounded-full bg-lime text-forest transition hover:shadow-md focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:outline-none active:scale-95 disabled:pointer-events-none disabled:opacity-40"
  >
    <span className={joinClassNames("grid grid-cols-[0fr] transition-[grid-template-columns] duration-250 ease-out", REVEAL_CLASS_NAME)}>
      <span className="overflow-hidden">
        <span className={joinClassNames("block pl-4 text-sm font-semibold whitespace-nowrap opacity-0 transition-opacity duration-200", LABEL_SHOWN_CLASS_NAME)}>
          {label}
        </span>
      </span>
    </span>
    <span className="grid size-9 shrink-0 place-items-center">
      <Icon name="plus" className="size-4.5 transition-transform duration-300 group-hover/add:rotate-90" />
    </span>
  </button>
);
