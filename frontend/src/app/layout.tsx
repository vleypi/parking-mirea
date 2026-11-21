import { QueryProvider } from "@/shared/query";
import type { Metadata } from "next";
import { Onest, Open_Sans } from "next/font/google";
import "./globals.css";

const openSans = Open_Sans({ variable: "--font-open-sans", subsets: ["latin", "cyrillic"] });
const onest = Onest({ variable: "--font-onest", subsets: ["latin", "cyrillic"] });

export const metadata: Metadata = {
  title: { default: "Парковка", template: "%s · Парковка" },
  description: "Информационная система парковки: заявки, владельцы, автомобили и места",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ru" className={`${openSans.variable} ${onest.variable}`}>
      <body>
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}
