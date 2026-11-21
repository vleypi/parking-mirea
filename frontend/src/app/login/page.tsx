import type { Metadata } from "next";
import { LoginPage } from "@/views/login";

export const metadata: Metadata = { title: "Вход" };

export default async function Page({ searchParams }: PageProps<"/login">) {
  const { next } = await searchParams;

  return <LoginPage next={typeof next === "string" ? next : undefined} />;
}
