import type { Metadata } from "next";
import { OwnersPage } from "@/views/owners";

export const metadata: Metadata = { title: "Владельцы" };

export default function Page() {
  return <OwnersPage />;
}
