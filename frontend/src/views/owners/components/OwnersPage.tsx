"use client";

import { ROLES } from "@/shared/config";
import { RequireRole } from "@/entities/viewer";
import { OwnersCatalog } from "@/views/owners/components/OwnersCatalog";

export const OwnersPage = () => (
  <RequireRole minRole={ROLES.viewer}>
    <OwnersCatalog />
  </RequireRole>
);
