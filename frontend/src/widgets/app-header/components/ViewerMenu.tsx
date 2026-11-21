"use client";

import { useId } from "react";
import { ROLE_LABELS } from "@/shared/config";
import { Avatar, HeaderPopover } from "@/shared/ui";
import { getTopRole, useViewer } from "@/entities/viewer";
import { LogoutButton } from "@/features/logout";
import { APP_HEADER_TEXT } from "@/widgets/app-header/constants/app-header.constant";

export const ViewerMenu = () => {
  const { data: viewer } = useViewer();
  const menuId = useId();

  if (!viewer) return <span className="size-9 shrink-0 animate-pulse rounded-full bg-gray-200" />;

  return (
    <>
      <button type="button" popoverTarget={menuId} aria-label={APP_HEADER_TEXT.profileMenu} className="shrink-0 rounded-full transition hover:opacity-80">
        <Avatar name={viewer.displayName} />
      </button>
      <HeaderPopover id={menuId} className="right-4 w-64 p-3 md:right-6">
        <div className="flex items-center gap-3 px-2 py-1">
          <Avatar name={viewer.displayName} size="sm" />
          <span className="min-w-0">
            <span className="block truncate text-sm font-medium text-ink">{viewer.displayName}</span>
            <span className="block truncate text-xs text-gray-500">{ROLE_LABELS[getTopRole(viewer)]}</span>
          </span>
        </div>
        <div className="my-2 h-px bg-gray-100" />
        <LogoutButton />
      </HeaderPopover>
    </>
  );
};
