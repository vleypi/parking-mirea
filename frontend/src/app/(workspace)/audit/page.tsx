import type { Metadata } from "next";
import { AuditPage } from "@/views/audit";

export const metadata: Metadata = { title: "Аудит" };

export default function Page() {
  return <AuditPage />;
}
