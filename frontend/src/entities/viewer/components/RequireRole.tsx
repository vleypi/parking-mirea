"use client";

import type { ReactNode } from "react";
import type { Role } from "@/shared/config";
import { EmptyState } from "@/shared/ui";
import { VIEWER_TEXT } from "@/entities/viewer/constants/viewer.constant";
import { useViewer } from "@/entities/viewer/hooks/useViewer";
import { hasRole } from "@/entities/viewer/utils/viewer-access.util";

interface IRequireRoleProps {
  minRole: Role;
  children: ReactNode;
}

export const RequireRole = ({ minRole, children }: IRequireRoleProps) => {
  const { data: viewer, isPending } = useViewer();

  if (isPending) return null;
  if (!hasRole(viewer, minRole)) {
    return <EmptyState icon="lock" title={VIEWER_TEXT.forbiddenTitle} description={VIEWER_TEXT.forbiddenDescription} />;
  }

  return children;
};
