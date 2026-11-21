import { type ComponentProps, useId } from "react";

interface ICheckboxFieldProps extends Omit<ComponentProps<"input">, "type"> {
  label: string;
  description?: string;
}

export const CheckboxField = ({ label, description, ...props }: ICheckboxFieldProps) => {
  const id = useId();

  return (
    <label htmlFor={id} className="flex cursor-pointer items-start gap-3 rounded-lg px-1 py-1.5">
      <input id={id} type="checkbox" className="mt-0.5 size-4 shrink-0 accent-brand" {...props} />
      <span className="flex flex-col">
        <span className="text-sm font-medium text-ink">{label}</span>
        {description && <span className="text-xs text-gray-500">{description}</span>}
      </span>
    </label>
  );
};
