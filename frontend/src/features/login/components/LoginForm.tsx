"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { applyServerErrors } from "@/shared/lib";
import { Button, FormError, TextField } from "@/shared/ui";
import { useForm } from "react-hook-form";
import { LOGIN_TEXT } from "@/features/login/constants/login.constant";
import { useLogin } from "@/features/login/hooks/useLogin";
import { loginFormSchema, type LoginFormValues } from "@/features/login/schemas/login.schema";

interface ILoginFormProps {
  next?: string;
}

export const LoginForm = ({ next }: ILoginFormProps) => {
  const { mutate: login, isPending } = useLogin(next);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginFormSchema), defaultValues: { login: "", password: "" } });

  const onSubmit = (credentials: LoginFormValues) => login(credentials, { onError: (error) => applyServerErrors(error, setError) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5" noValidate>
      <TextField label={LOGIN_TEXT.login} autoComplete="username" autoFocus error={errors.login?.message} {...register("login")} />
      <TextField
        label={LOGIN_TEXT.password}
        type="password"
        autoComplete="current-password"
        error={errors.password?.message}
        {...register("password")}
      />
      <FormError message={errors.root?.server?.message} />
      <Button type="submit" isLoading={isPending} className="mt-2 w-full py-3">
        {isPending ? LOGIN_TEXT.submitting : LOGIN_TEXT.submit}
      </Button>
    </form>
  );
};
