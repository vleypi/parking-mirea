import type { Metadata } from "next";
import { VehiclesPage } from "@/views/vehicles";

export const metadata: Metadata = { title: "Автомобили" };

export default function Page() {
  return <VehiclesPage />;
}
