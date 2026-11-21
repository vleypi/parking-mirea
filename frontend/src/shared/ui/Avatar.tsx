import { joinClassNames } from "@/shared/lib";

type AvatarSize = "sm" | "md";

interface IAvatarProps {
  name: string;
  size?: AvatarSize;
}

const SIZE_CLASSES: Record<AvatarSize, string> = {
  sm: "size-8 text-xs",
  md: "size-9 text-sm",
};

const INITIALS_LENGTH = 2;

const toInitials = (name: string): string =>
  name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, INITIALS_LENGTH)
    .map((word) => word[0].toUpperCase())
    .join("");

export const Avatar = ({ name, size = "md" }: IAvatarProps) => (
  <span aria-hidden className={joinClassNames("grid shrink-0 place-items-center rounded-full bg-brand-soft font-semibold text-brand", SIZE_CLASSES[size])}>
    {toInitials(name)}
  </span>
);
