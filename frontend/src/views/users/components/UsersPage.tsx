"use client";

import { ROLES } from "@/shared/config";
import { RequireRole } from "@/entities/viewer";
import { UsersCatalog } from "@/views/users/components/UsersCatalog";

export const UsersPage = () => (
  <RequireRole minRole={ROLES.admin}>
    <UsersCatalog />
  </RequireRole>
);
