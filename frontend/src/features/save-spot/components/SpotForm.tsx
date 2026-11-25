"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyServerErrors } from "@/shared/lib";
import { Button, CheckboxField, FormError, ModalFooter, SelectField, TextAreaField, TextField } from "@/shared/ui";
import { type ISpotType, useReferences } from "@/entities/reference";
import { type ISpot, SPOT_STATUS_LABELS, SPOT_STATUSES } from "@/entities/spot";
import { SAVE_SPOT_TEXT } from "@/features/save-spot/constants/save-spot.constant";
import { useSaveSpot } from "@/features/save-spot/hooks/useSaveSpot";
import { spotFormSchema, type SpotFormValues } from "@/features/save-spot/schemas/spot-form.schema";
import { toSpotFormValues } from "@/features/save-spot/utils/spot-form.util";

interface ISpotFormProps {
  spot: ISpot | null;
  onDone?: () => void;
}

const STATUS_OPTIONS = Object.values(SPOT_STATUSES).map((status) => ({ value: status, label: SPOT_STATUS_LABELS[status] }));

/** Отключённый тип выбрать нельзя, но текущий тип места оставляем в списке. */
const toTypeOptions = (types: ISpotType[] | undefined, spot: ISpot | null) => [
  { value: "", label: SAVE_SPOT_TEXT.typePlaceholder },
  ...(types ?? []).filter((type) => type.active || type.id === spot?.spotType.id).map((type) => ({ value: String(type.id), label: type.title })),
];

export const SpotForm = ({ spot, onDone }: ISpotFormProps) => {
  const isNew = spot === null;
  const { data: references } = useReferences();
  const { mutate: save, isPending } = useSaveSpot(spot);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isDirty },
  } = useForm<SpotFormValues>({ resolver: zodResolver(spotFormSchema), values: toSpotFormValues(spot) });

  const onSubmit = (values: SpotFormValues) =>
    save(values, { onSuccess: () => onDone?.(), onError: (error) => applyServerErrors(error, setError) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label={SAVE_SPOT_TEXT.spotNumber} inputMode="numeric" error={errors.spotNumber?.message} {...register("spotNumber")} />
        <SelectField
          label={SAVE_SPOT_TEXT.type}
          options={toTypeOptions(references?.spotTypes, spot)}
          error={errors.spotTypeId?.message}
          {...register("spotTypeId")}
        />
        <TextField
          label={SAVE_SPOT_TEXT.rate}
          hint={SAVE_SPOT_TEXT.rateHint}
          inputMode="decimal"
          error={errors.hourlyRate?.message}
          {...register("hourlyRate")}
        />
        <SelectField label={SAVE_SPOT_TEXT.status} options={STATUS_OPTIONS} error={errors.status?.message} {...register("status")} />
        <TextField label={SAVE_SPOT_TEXT.zone} hint={SAVE_SPOT_TEXT.zoneHint} error={errors.zone?.message} {...register("zone")} />
        <TextField label={SAVE_SPOT_TEXT.tags} hint={SAVE_SPOT_TEXT.tagsHint} error={errors.tags?.message} {...register("tags")} />
      </div>
      <TextAreaField label={SAVE_SPOT_TEXT.description} rows={3} error={errors.description?.message} {...register("description")} />
      <CheckboxField label={SAVE_SPOT_TEXT.covered} {...register("covered")} />
      <FormError message={errors.root?.server?.message} />
      {isNew ? (
        <ModalFooter>
          <Button variant="secondary" onClick={onDone} disabled={isPending}>
            {SAVE_SPOT_TEXT.cancel}
          </Button>
          <Button type="submit" isLoading={isPending}>
            {SAVE_SPOT_TEXT.create}
          </Button>
        </ModalFooter>
      ) : (
        <div className="flex justify-end">
          <Button type="submit" isLoading={isPending} disabled={!isDirty}>
            {SAVE_SPOT_TEXT.save}
          </Button>
        </div>
      )}
    </form>
  );
};
