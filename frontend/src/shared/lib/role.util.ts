import { type Role, ROLE_RANK } from "@/shared/config";

export const getTopRole = (roles: readonly Role[]): Role => roles.reduce((top, role) => (ROLE_RANK[role] > ROLE_RANK[top] ? role : top));

export const hasMinRole = (roles: readonly Role[], minRole: Role): boolean => roles.some((role) => ROLE_RANK[role] >= ROLE_RANK[minRole]);
