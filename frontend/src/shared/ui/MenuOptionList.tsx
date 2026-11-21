import { joinClassNames } from "@/shared/lib";
import { Icon } from "@/shared/ui/Icon";
import { type ISelectOption } from "@/shared/ui/SelectControl";
import { MENU_ITEM_CLASS_NAME } from "@/shared/ui/ui.constant";

interface IMenuOptionListProps {
  menuId: string;
  options: readonly ISelectOption[];
  value: string | undefined;
  onSelect: (value: string) => void;
}

export const MenuOptionList = ({ menuId, options, value, onSelect }: IMenuOptionListProps) => (
  <ul>
    {options.map((option) => {
      const isCurrent = option.value === value;

      return (
        <li key={option.value}>
          <button
            type="button"
            popoverTarget={menuId}
            popoverTargetAction="hide"
            aria-current={isCurrent}
            onClick={() => onSelect(option.value)}
            className={joinClassNames(MENU_ITEM_CLASS_NAME, "justify-between aria-[current=true]:font-semibold aria-[current=true]:text-brand")}
          >
            <span className="truncate">{option.label}</span>
            {isCurrent && <Icon name="check" className="size-4" />}
          </button>
        </li>
      );
    })}
  </ul>
);
