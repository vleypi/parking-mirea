import type { ComponentProps } from "react";
import { joinClassNames } from "@/shared/lib/join-class-names";
import { CONTROL_BORDER_CLASSES, CONTROL_CLASS_NAME } from "@/shared/ui/field.constant";

export interface ISelectOption {
  value: string;
  label: string;
}

interface ISelectControlProps extends ComponentProps<"select"> {
  options: readonly ISelectOption[];
  isInvalid?: boolean;
}

export const SelectControl = ({ options, isInvalid = false, className, ...props }: ISelectControlProps) => (
  <select
    aria-invalid={isInvalid}
    className={joinClassNames(CONTROL_CLASS_NAME, CONTROL_BORDER_CLASSES[isInvalid ? "invalid" : "valid"], "appearance-auto", className)}
    {...props}
  >
    {options.map(({ value, label }) => (
      <option key={value} value={value}>
        {label}
      </option>
    ))}
  </select>
);
