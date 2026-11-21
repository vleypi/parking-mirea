import { type ComponentProps, useId } from "react";
import { joinClassNames } from "@/shared/lib/join-class-names";
import { Field } from "@/shared/ui/Field";
import { CONTROL_BORDER_CLASSES, CONTROL_CLASS_NAME } from "@/shared/ui/field.constant";

interface ITextAreaFieldProps extends ComponentProps<"textarea"> {
  label: string;
  error?: string;
  hint?: string;
}

export const TextAreaField = ({ label, error, hint, className, ...props }: ITextAreaFieldProps) => {
  const id = useId();

  return (
    <Field id={id} label={label} error={error} hint={hint}>
      <textarea
        id={id}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        className={joinClassNames(CONTROL_CLASS_NAME, CONTROL_BORDER_CLASSES[error ? "invalid" : "valid"], "min-h-28 resize-y", className)}
        {...props}
      />
    </Field>
  );
};
