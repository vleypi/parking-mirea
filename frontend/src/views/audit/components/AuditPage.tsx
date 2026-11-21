"use client";

import { ROLES } from "@/shared/config";
import { RequireRole } from "@/entities/viewer";
import { AuditCatalog } from "@/views/audit/components/AuditCatalog";

export const AuditPage = () => (
  <RequireRole minRole={ROLES.admin}>
    <AuditCatalog />
  </RequireRole>
);
