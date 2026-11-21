import type { Metadata } from "next";
import { RequestsPage } from "@/views/requests";

export const metadata: Metadata = { title: "Заявки" };

export default function Page() {
  return <RequestsPage />;
}
