import type { Metadata } from "next";
import { UsersPage } from "@/views/users";

export const metadata: Metadata = { title: "Пользователи" };

export default function Page() {
  return <UsersPage />;
}
