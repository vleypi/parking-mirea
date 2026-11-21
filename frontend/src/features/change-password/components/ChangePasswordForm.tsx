"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyServerErrors } from "@/shared/lib";
import { Button, FormError, TextField } from "@/shared/ui";
import { CHANGE_PASSWORD_MODE_TEXT, CHANGE_PASSWORD_TEXT, type ChangePasswordMode } from "@/features/change-password/constants/change-password.constant";
import { useChangePassword } from "@/features/change-password/hooks/useChangePassword";
import { changePasswordFormSchema, type ChangePasswordFormValues } from "@/features/change-password/schemas/change-password.schema";

interface IChangePasswordFormProps {
  mode: ChangePasswordMode;
  onDone?: () => void;
}

export const ChangePasswordForm = ({ mode, onDone }: IChangePasswordFormProps) => {
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

  const onSubmit = (values: ChangePasswordFormValues) =>
    changePassword(values, { onSuccess: onDone, onError: (error) => applyServerErrors(error, setError) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
      <TextField
        label={CHANGE_PASSWORD_MODE_TEXT[mode].currentPassword}
        type="password"
        autoComplete="current-password"
        error={errors.currentPassword?.message}
        {...register("currentPassword")}
      />
      <TextField
        label={CHANGE_PASSWORD_TEXT.newPassword}
        hint={CHANGE_PASSWORD_TEXT.newPasswordHint}
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
  );
};
