import type { Metadata } from "next";
import { ROLES } from "@/shared/config";
import { SectionPlaceholderPage } from "@/views/section-placeholder";

const TITLE = "Справочники";

export const metadata: Metadata = { title: TITLE };

export default function Page() {
  return <SectionPlaceholderPage title={TITLE} minRole={ROLES.viewer} />;
}
