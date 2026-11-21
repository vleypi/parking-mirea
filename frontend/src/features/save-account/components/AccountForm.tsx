"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyServerErrors } from "@/shared/lib";
import { Button, CheckboxField, FormError, ModalFooter, SelectField, TextField } from "@/shared/ui";
import { ACCOUNT_ROLE_OPTIONS, type IAccount, type ITemporaryPassword } from "@/entities/account";
import { SAVE_ACCOUNT_TEXT } from "@/features/save-account/constants/save-account.constant";
import { useSaveAccount } from "@/features/save-account/hooks/useSaveAccount";
import { accountFormSchema, type AccountFormValues } from "@/features/save-account/schemas/account-form.schema";
import { toAccountFormValues } from "@/features/save-account/utils/account-form.util";

interface IAccountFormProps {
  account: IAccount | null;
  onCreated: (issued: ITemporaryPassword) => void;
  onDone: () => void;
}

export const AccountForm = ({ account, onCreated, onDone }: IAccountFormProps) => {
  const isNew = account === null;
  const { mutate: save, isPending } = useSaveAccount(account);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<AccountFormValues>({ resolver: zodResolver(accountFormSchema), defaultValues: toAccountFormValues(account) });

  const onSubmit = (values: AccountFormValues) =>
    save(values, {
      onSuccess: (result) => ("temporaryPassword" in result ? onCreated(result) : onDone()),
      onError: (error) => applyServerErrors(error, setError),
    });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label={SAVE_ACCOUNT_TEXT.displayName} error={errors.displayName?.message} {...register("displayName")} />
        <TextField label={SAVE_ACCOUNT_TEXT.login} hint={SAVE_ACCOUNT_TEXT.loginHint} autoComplete="off" error={errors.login?.message} {...register("login")} />
        <SelectField label={SAVE_ACCOUNT_TEXT.role} options={ACCOUNT_ROLE_OPTIONS} error={errors.role?.message} {...register("role")} />
      </div>
      {!isNew && <CheckboxField label={SAVE_ACCOUNT_TEXT.enabled} description={SAVE_ACCOUNT_TEXT.enabledHint} {...register("enabled")} />}
      <FormError message={errors.root?.server?.message} />
      <ModalFooter>
        <Button variant="secondary" onClick={onDone} disabled={isPending}>
          {SAVE_ACCOUNT_TEXT.cancel}
        </Button>
        <Button type="submit" isLoading={isPending}>
          {isNew ? SAVE_ACCOUNT_TEXT.create : SAVE_ACCOUNT_TEXT.save}
        </Button>
      </ModalFooter>
    </form>
  );
};
