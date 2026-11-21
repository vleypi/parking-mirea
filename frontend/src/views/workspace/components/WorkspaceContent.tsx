"use client";

import { FormError } from "@/shared/ui";
import type { ReactNode } from "react";
import { useViewer } from "@/entities/viewer";
import { TemporaryPasswordScreen } from "@/features/change-password";
import { WORKSPACE_TEXT } from "@/views/workspace/constants/workspace.constant";

interface IWorkspaceContentProps {
  children: ReactNode;
}

export const WorkspaceContent = ({ children }: IWorkspaceContentProps) => {
  const { data: viewer, isPending, isError } = useViewer();

  if (isPending) return null;
  if (isError) return <FormError message={WORKSPACE_TEXT.loadFailed} />;
  if (viewer.mustChangePassword) return <TemporaryPasswordScreen />;

  return children;
};
