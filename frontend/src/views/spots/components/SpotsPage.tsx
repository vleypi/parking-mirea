"use client";

import { ROLES } from "@/shared/config";
import { RequireRole } from "@/entities/viewer";
import { SpotsCatalog } from "@/views/spots/components/SpotsCatalog";

export const SpotsPage = () => (
  <RequireRole minRole={ROLES.viewer}>
    <SpotsCatalog />
  </RequireRole>
);
