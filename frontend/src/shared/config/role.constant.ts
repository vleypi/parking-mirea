export const ROLES = {
  viewer: "VIEWER",
  operator: "OPERATOR",
  admin: "ADMIN",
} as const;

export type Role = (typeof ROLES)[keyof typeof ROLES];

export const ROLE_LABELS: Record<Role, string> = {
  VIEWER: "Наблюдатель",
  OPERATOR: "Оператор",
  ADMIN: "Администратор",
};

export const ROLE_RANK: Record<Role, number> = {
  VIEWER: 1,
  OPERATOR: 2,
  ADMIN: 3,
};
