"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyServerErrors } from "@/shared/lib";
import { Button, CheckboxField, FormError, ModalFooter, SelectField, TextField } from "@/shared/ui";
import { type IClientCategory, type IOwner, OWNER_STATUS_LABELS, OWNER_STATUSES, useClientCategories } from "@/entities/owner";
import { SAVE_OWNER_TEXT } from "@/features/save-owner/constants/save-owner.constant";
import { useSaveOwner } from "@/features/save-owner/hooks/useSaveOwner";
import { ownerFormSchema, type OwnerFormValues } from "@/features/save-owner/schemas/owner-form.schema";
import { toOwnerFormValues } from "@/features/save-owner/utils/owner-form.util";

interface IOwnerFormProps {
  owner: IOwner | null;
  onDone?: () => void;
}

const STATUS_OPTIONS = Object.values(OWNER_STATUSES).map((status) => ({ value: status, label: OWNER_STATUS_LABELS[status] }));

/** Отключённую категорию выбрать нельзя, но текущую категорию владельца оставляем в списке. */
const toCategoryOptions = (categories: IClientCategory[] | undefined, owner: IOwner | null) =>
  (categories ?? [])
    .filter((category) => category.active || category.id === owner?.category.id)
    .map((category) => ({ value: String(category.id), label: category.title }));

export const OwnerForm = ({ owner, onDone }: IOwnerFormProps) => {
  const isNew = owner === null;
  const { data: categories } = useClientCategories();
  const { mutate: save, isPending } = useSaveOwner(owner);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isDirty },
  } = useForm<OwnerFormValues>({ resolver: zodResolver(ownerFormSchema), values: toOwnerFormValues(owner) });

  const onSubmit = (values: OwnerFormValues) =>
    save(values, { onSuccess: () => onDone?.(), onError: (error) => applyServerErrors(error, setError) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label={SAVE_OWNER_TEXT.name} error={errors.name?.message} {...register("name")} />
        <TextField label={SAVE_OWNER_TEXT.phone} hint={SAVE_OWNER_TEXT.phoneHint} inputMode="tel" error={errors.phone?.message} {...register("phone")} />
        <TextField label={SAVE_OWNER_TEXT.email} hint={SAVE_OWNER_TEXT.emailHint} type="email" error={errors.email?.message} {...register("email")} />
        <SelectField
          label={SAVE_OWNER_TEXT.category}
          options={toCategoryOptions(categories, owner)}
          error={errors.categoryId?.message}
          {...register("categoryId")}
        />
        <SelectField label={SAVE_OWNER_TEXT.status} options={STATUS_OPTIONS} error={errors.status?.message} {...register("status")} />
        <TextField
          label={SAVE_OWNER_TEXT.discount}
          hint={SAVE_OWNER_TEXT.discountHint}
          inputMode="decimal"
          error={errors.discountPercent?.message}
          {...register("discountPercent")}
        />
      </div>
      <TextField label={SAVE_OWNER_TEXT.tags} hint={SAVE_OWNER_TEXT.tagsHint} error={errors.tags?.message} {...register("tags")} />
      <CheckboxField label={SAVE_OWNER_TEXT.notifications} {...register("notificationsEnabled")} />
      <FormError message={errors.root?.server?.message} />
      {isNew ? (
        <ModalFooter>
          <Button variant="secondary" onClick={onDone} disabled={isPending}>
            {SAVE_OWNER_TEXT.cancel}
          </Button>
          <Button type="submit" isLoading={isPending}>
            {SAVE_OWNER_TEXT.create}
          </Button>
        </ModalFooter>
      ) : (
        <div className="flex justify-end">
          <Button type="submit" isLoading={isPending} disabled={!isDirty}>
            {SAVE_OWNER_TEXT.save}
          </Button>
        </div>
      )}
    </form>
  );
};
