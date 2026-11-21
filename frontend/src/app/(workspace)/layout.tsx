import { cookies } from "next/headers";
import { COOKIE_KEYS } from "@/shared/config";
import { WorkspaceLayout } from "@/views/workspace";

export default async function Layout({ children }: LayoutProps<"/">) {
  const cookieStore = await cookies();

  return <WorkspaceLayout initialSidebarExpanded={cookieStore.get(COOKIE_KEYS.sidebarOpen)?.value !== "false"}>{children}</WorkspaceLayout>;
}
