"use client";

import { ROLES } from "@/shared/config";
import { RequireRole } from "@/entities/viewer";
import { VehiclesCatalog } from "@/views/vehicles/components/VehiclesCatalog";

export const VehiclesPage = () => (
  <RequireRole minRole={ROLES.viewer}>
    <VehiclesCatalog />
  </RequireRole>
);
