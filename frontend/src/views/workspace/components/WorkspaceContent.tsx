"use client";

import { FormError } from "@/shared/ui";
import type { ReactNode } from "react";
import { useViewer } from "@/entities/viewer";
import { ChangePasswordForm } from "@/features/change-password";
import { WORKSPACE_TEXT } from "@/views/workspace/constants/workspace.constant";

interface IWorkspaceContentProps {
  children: ReactNode;
}

export const WorkspaceContent = ({ children }: IWorkspaceContentProps) => {
  const { data: viewer, isPending, isError } = useViewer();

  if (isPending) return null;
  if (isError) return <FormError message={WORKSPACE_TEXT.loadFailed} />;
  if (viewer.mustChangePassword) {
    return (
      <div className="grid min-h-full place-items-center">
        <ChangePasswordForm />
      </div>
    );
  }

  return children;
};
