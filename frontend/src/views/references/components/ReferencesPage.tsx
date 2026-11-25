"use client";

import { ROLES } from "@/shared/config";
import { RequireRole } from "@/entities/viewer";
import { ReferencesCatalog } from "@/views/references/components/ReferencesCatalog";

export const ReferencesPage = () => (
  <RequireRole minRole={ROLES.viewer}>
    <ReferencesCatalog />
  </RequireRole>
);
