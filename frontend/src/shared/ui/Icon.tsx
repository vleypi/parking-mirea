import { joinClassNames } from "@/shared/lib/join-class-names";
import { ICON_PATHS, type IconName } from "@/shared/ui/icons.constant";

interface IIconProps {
  name: IconName;
  className?: string;
}

const DEFAULT_ICON_SIZE = 20;

export const Icon = ({ name, className }: IIconProps) => (
  <svg
    width={DEFAULT_ICON_SIZE}
    height={DEFAULT_ICON_SIZE}
    className={joinClassNames("shrink-0", className)}
    fill="none"
    stroke="currentColor"
    viewBox="0 0 24 24"
    aria-hidden
  >
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={ICON_PATHS[name]} />
  </svg>
);
