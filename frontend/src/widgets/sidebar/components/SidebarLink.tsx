"use client";

import { joinClassNames } from "@/shared/lib";
import { Icon } from "@/shared/ui";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { INavItem } from "@/shared/config";
import { useSidebar } from "@/shared/ui";
import { NavTooltip } from "@/widgets/sidebar/components/NavTooltip";
import { useNavTooltip } from "@/widgets/sidebar/hooks/useNavTooltip";

interface ISidebarLinkProps {
  item: INavItem;
}

export const SidebarLink = ({ item: { href, label, icon } }: ISidebarLinkProps) => {
  const pathname = usePathname();
  const { closeMobile } = useSidebar();
  const { anchorRef, tooltipRef, show: showTooltip, hide: hideTooltip } = useNavTooltip();
  const isActive = pathname === href || pathname.startsWith(`${href}/`);

  const handleClick = () => {
    hideTooltip();
    closeMobile();
  };

  return (
    <Link
      ref={anchorRef}
      href={href}
      onClick={handleClick}
      onMouseEnter={showTooltip}
      onMouseLeave={hideTooltip}
      onFocus={showTooltip}
      onBlur={hideTooltip}
      aria-current={isActive ? "page" : undefined}
      className={joinClassNames(
        "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition",
        isActive ? "bg-brand-soft text-brand" : "text-gray-700 hover:bg-gray-100",
      )}
    >
      <Icon name={icon} />
      <span className="truncate whitespace-nowrap transition-opacity duration-200 md:group-data-[expanded=false]:opacity-0">{label}</span>
      <NavTooltip ref={tooltipRef} label={label} />
    </Link>
  );
};
