import type { ReactNode } from "react";
import { LABEL_CLASS_NAME } from "@/shared/ui/field.constant";

interface IFieldProps {
  id: string;
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}

export const Field = ({ id, label, error, hint, children }: IFieldProps) => (
  <div className="flex flex-col gap-1.5">
    <label htmlFor={id} className={LABEL_CLASS_NAME}>
      {label}
    </label>
    {children}
    {error ? (
      <p id={`${id}-error`} className="text-sm text-red-600">
        {error}
      </p>
    ) : (
      hint && <p className="text-xs text-gray-500">{hint}</p>
    )}
  </div>
);
