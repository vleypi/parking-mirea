import type { ComponentProps } from "react";
import { joinClassNames } from "@/shared/lib/join-class-names";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";

interface IButtonProps extends ComponentProps<"button"> {
  variant?: ButtonVariant;
  isLoading?: boolean;
}

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: "bg-brand text-white hover:bg-brand/90",
  secondary: "border border-gray-200 bg-white text-gray-700 hover:bg-gray-50",
  ghost: "text-gray-600 hover:bg-gray-100 hover:text-ink",
  danger: "text-red-700 hover:bg-red-50",
};

export const Button = ({ variant = "primary", isLoading = false, disabled, className, type = "button", ...props }: IButtonProps) => (
  <button
    type={type}
    disabled={disabled || isLoading}
    aria-busy={isLoading}
    className={joinClassNames(
      "inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50",
      VARIANT_CLASSES[variant],
      className,
    )}
    {...props}
  />
);
