"use client";

import type { ReactNode } from "react";
import { SidebarProvider } from "@/shared/ui";
import { AppHeader } from "@/widgets/app-header";
import { Sidebar } from "@/widgets/sidebar";
import { WorkspaceContent } from "@/views/workspace/components/WorkspaceContent";

interface IWorkspaceLayoutProps {
  initialSidebarExpanded: boolean;
  children: ReactNode;
}

export const WorkspaceLayout = ({ initialSidebarExpanded, children }: IWorkspaceLayoutProps) => (
  <SidebarProvider initialExpanded={initialSidebarExpanded}>
    <div className="flex h-dvh overflow-hidden bg-white">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col overflow-hidden bg-canvas md:my-2 md:mr-2 md:rounded-2xl md:border md:border-gray-200">
        <AppHeader />
        <main className="min-h-0 flex-1 overflow-y-auto px-4 py-6 md:px-8 lg:px-16">
          <div className="mx-auto flex min-h-full w-full max-w-6xl flex-col">
            <WorkspaceContent>{children}</WorkspaceContent>
          </div>
        </main>
      </div>
    </div>
  </SidebarProvider>
);
