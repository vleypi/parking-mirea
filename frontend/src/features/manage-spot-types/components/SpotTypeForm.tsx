"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyServerErrors } from "@/shared/lib";
import { Button, FormError, ModalFooter, TextField } from "@/shared/ui";
import type { ISpotType } from "@/entities/reference";
import { MANAGE_SPOT_TYPES_TEXT } from "@/features/manage-spot-types/constants/manage-spot-types.constant";
import { useSaveSpotType } from "@/features/manage-spot-types/hooks/useManageSpotTypes";
import { spotTypeFormSchema, type SpotTypeFormValues } from "@/features/manage-spot-types/schemas/spot-type-form.schema";

interface ISpotTypeFormProps {
  spotType: ISpotType | null;
  onDone: () => void;
}

const toFormValues = (spotType: ISpotType | null): SpotTypeFormValues => ({
  code: spotType?.code ?? "",
  title: spotType?.title ?? "",
  sortOrder: spotType ? String(spotType.sortOrder) : "",
});

export const SpotTypeForm = ({ spotType, onDone }: ISpotTypeFormProps) => {
  const { mutate: save, isPending } = useSaveSpotType(spotType);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<SpotTypeFormValues>({ resolver: zodResolver(spotTypeFormSchema), values: toFormValues(spotType) });

  const onSubmit = (values: SpotTypeFormValues) => save(values, { onSuccess: onDone, onError: (error) => applyServerErrors(error, setError) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
      <TextField label={MANAGE_SPOT_TYPES_TEXT.code} hint={MANAGE_SPOT_TYPES_TEXT.codeHint} error={errors.code?.message} {...register("code")} />
      <TextField label={MANAGE_SPOT_TYPES_TEXT.title} error={errors.title?.message} {...register("title")} />
      <TextField
        label={MANAGE_SPOT_TYPES_TEXT.sortOrder}
        hint={MANAGE_SPOT_TYPES_TEXT.sortOrderHint}
        inputMode="numeric"
        error={errors.sortOrder?.message}
        {...register("sortOrder")}
      />
      <FormError message={errors.root?.server?.message} />
      <ModalFooter>
        <Button variant="secondary" onClick={onDone} disabled={isPending}>
          {MANAGE_SPOT_TYPES_TEXT.cancel}
        </Button>
        <Button type="submit" isLoading={isPending}>
          {spotType ? MANAGE_SPOT_TYPES_TEXT.save : MANAGE_SPOT_TYPES_TEXT.create}
        </Button>
      </ModalFooter>
    </form>
  );
};
