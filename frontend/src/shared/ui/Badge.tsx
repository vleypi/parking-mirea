import { joinClassNames } from "@/shared/lib";
import type { ReactNode } from "react";

export type BadgeTone = "neutral" | "brand" | "success" | "warning" | "danger";

interface IBadgeProps {
  tone?: BadgeTone;
  children: ReactNode;
}

const TONE_CLASSES: Record<BadgeTone, string> = {
  neutral: "bg-gray-100 text-gray-600",
  brand: "bg-brand-soft text-brand",
  success: "bg-green-50 text-green-700",
  warning: "bg-amber-50 text-amber-700",
  danger: "bg-red-50 text-red-700",
};

export const Badge = ({ tone = "neutral", children }: IBadgeProps) => (
  <span className={joinClassNames("inline-flex rounded-md px-2 py-0.5 text-xs font-medium whitespace-nowrap", TONE_CLASSES[tone])}>{children}</span>
);
