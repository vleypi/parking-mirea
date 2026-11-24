import type { Metadata } from "next";
import { AnalyticsPage } from "@/views/analytics";

export const metadata: Metadata = { title: "Аналитика" };

export default function Page() {
  return <AnalyticsPage />;
}
