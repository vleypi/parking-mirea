import { Logo } from "@/shared/ui";
import { LoginForm } from "@/features/login";
import { LOGIN_PAGE_TEXT } from "@/views/login/constants/login-page.constant";

interface ILoginPageProps {
  next?: string;
}

export const LoginPage = ({ next }: ILoginPageProps) => (
  <div className="grid min-h-dvh bg-white lg:grid-cols-2">
    <main className="flex flex-col px-6 py-8 sm:px-12">
      <Logo />
      <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center py-12">
        <h1 className="text-3xl leading-tight font-bold tracking-tight text-ink">{LOGIN_PAGE_TEXT.title}</h1>
        <p className="mt-2 mb-10 text-sm text-gray-500">{LOGIN_PAGE_TEXT.description}</p>
        <LoginForm next={next} />
      </div>
    </main>
    <aside className="relative hidden overflow-hidden bg-forest lg:flex lg:items-end lg:p-16">
      <div className="absolute -top-24 -right-40 h-40 w-[140%] -rotate-12 bg-lime" />
      <p className="relative max-w-md text-3xl leading-snug font-semibold text-white">{LOGIN_PAGE_TEXT.tagline}</p>
    </aside>
  </div>
);
