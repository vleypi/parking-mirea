import type { Metadata } from "next";
import { SpotsPage } from "@/views/spots";

export const metadata: Metadata = { title: "Места" };

export default function Page() {
  return <SpotsPage />;
}
