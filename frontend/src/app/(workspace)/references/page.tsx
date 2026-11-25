import type { Metadata } from "next";
import { ReferencesPage } from "@/views/references";

export const metadata: Metadata = { title: "Справочники" };

export default function Page() {
  return <ReferencesPage />;
}
