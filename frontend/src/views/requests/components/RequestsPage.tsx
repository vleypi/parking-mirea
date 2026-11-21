"use client";

import { ROLES } from "@/shared/config";
import { RequireRole } from "@/entities/viewer";
import { RequestsCatalog } from "@/views/requests/components/RequestsCatalog";

export const RequestsPage = () => (
  <RequireRole minRole={ROLES.viewer}>
    <RequestsCatalog />
  </RequireRole>
);
