import type { Role } from "@/shared/config";

export interface IViewer {
  id: string;
  login: string;
  displayName: string;
  roles: Role[];
  mustChangePassword: boolean;
}
