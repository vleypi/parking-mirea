import { type ComponentProps, useId } from "react";
import { Field } from "@/shared/ui/Field";
import { type ISelectOption, SelectControl } from "@/shared/ui/SelectControl";

interface ISelectFieldProps extends ComponentProps<"select"> {
  label: string;
  options: readonly ISelectOption[];
  error?: string;
  hint?: string;
}

export const SelectField = ({ label, options, error, hint, ...props }: ISelectFieldProps) => {
  const id = useId();

  return (
    <Field id={id} label={label} error={error} hint={hint}>
      <SelectControl id={id} options={options} isInvalid={Boolean(error)} {...props} />
    </Field>
  );
};
