"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { applyServerErrors } from "@/shared/lib";
import { Button, FormError, TextField } from "@/shared/ui";
import { useForm } from "react-hook-form";
import { CHANGE_PASSWORD_TEXT } from "@/features/change-password/constants/change-password.constant";
import { useChangePassword } from "@/features/change-password/hooks/useChangePassword";
import { changePasswordFormSchema, type ChangePasswordFormValues } from "@/features/change-password/schemas/change-password.schema";

export const ChangePasswordForm = () => {
  const { mutate: changePassword, isPending } = useChangePassword();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordFormSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  const onSubmit = (values: ChangePasswordFormValues) => changePassword(values, { onError: (error) => applyServerErrors(error, setError) });

  return (
    <div className="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
      <h1 className="text-xl font-bold text-ink">{CHANGE_PASSWORD_TEXT.title}</h1>
      <p className="mt-1 mb-6 text-sm text-gray-500">{CHANGE_PASSWORD_TEXT.description}</p>
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <TextField
          label={CHANGE_PASSWORD_TEXT.currentPassword}
          type="password"
          autoComplete="current-password"
          error={errors.currentPassword?.message}
          {...register("currentPassword")}
        />
        <TextField
          label={CHANGE_PASSWORD_TEXT.newPassword}
          type="password"
          autoComplete="new-password"
          error={errors.newPassword?.message}
          {...register("newPassword")}
        />
        <TextField
          label={CHANGE_PASSWORD_TEXT.confirmPassword}
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register("confirmPassword")}
        />
        <FormError message={errors.root?.server?.message} />
        <Button type="submit" isLoading={isPending} className="mt-2 w-full py-3">
          {CHANGE_PASSWORD_TEXT.submit}
        </Button>
      </form>
    </div>
  );
};
