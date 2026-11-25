"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { applyServerErrors } from "@/shared/lib";
import { Button, CheckboxField, FormError, ModalFooter, SelectField, TextField } from "@/shared/ui";
import { type IVehicle, type IVehicleOwner, VEHICLE_STATUS_LABELS, VEHICLE_STATUSES } from "@/entities/vehicle";
import { OwnerPicker } from "@/features/save-vehicle/components/OwnerPicker";
import { SAVE_VEHICLE_TEXT } from "@/features/save-vehicle/constants/save-vehicle.constant";
import { useSaveVehicle } from "@/features/save-vehicle/hooks/useSaveVehicle";
import { type VehicleFormInput, vehicleFormSchema, type VehicleFormValues } from "@/features/save-vehicle/schemas/vehicle-form.schema";
import { toVehicleFormValues } from "@/features/save-vehicle/utils/vehicle-form.util";

interface IVehicleFormProps {
  vehicle: IVehicle | null;
  onDone?: () => void;
}

const STATUS_OPTIONS = Object.values(VEHICLE_STATUSES).map((status) => ({ value: status, label: VEHICLE_STATUS_LABELS[status] }));

export const VehicleForm = ({ vehicle, onDone }: IVehicleFormProps) => {
  const isNew = vehicle === null;
  const [owner, setOwner] = useState<IVehicleOwner | null>(vehicle?.owner ?? null);
  const { mutate: save, isPending } = useSaveVehicle(vehicle);
  const {
    register,
    handleSubmit,
    setError,
    setValue,
    formState: { errors, isDirty },
  } = useForm<VehicleFormInput, unknown, VehicleFormValues>({ resolver: zodResolver(vehicleFormSchema), values: toVehicleFormValues(vehicle) });

  const selectOwner = (next: IVehicleOwner) => {
    setOwner(next);
    setValue("ownerId", next.id, { shouldDirty: true, shouldValidate: true });
  };

  const onSubmit = (values: VehicleFormValues) =>
    save(values, { onSuccess: () => onDone?.(), onError: (error) => applyServerErrors(error, setError) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
      <OwnerPicker selected={owner} error={errors.ownerId?.message} onSelect={selectOwner} />
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField
          label={SAVE_VEHICLE_TEXT.licensePlate}
          hint={SAVE_VEHICLE_TEXT.licensePlateHint}
          autoCapitalize="characters"
          error={errors.licensePlate?.message}
          {...register("licensePlate")}
        />
        <SelectField label={SAVE_VEHICLE_TEXT.status} options={STATUS_OPTIONS} error={errors.status?.message} {...register("status")} />
        <TextField label={SAVE_VEHICLE_TEXT.brand} error={errors.brand?.message} {...register("brand")} />
        <TextField label={SAVE_VEHICLE_TEXT.model} error={errors.model?.message} {...register("model")} />
        <TextField label={SAVE_VEHICLE_TEXT.year} hint={SAVE_VEHICLE_TEXT.yearHint} inputMode="numeric" error={errors.releaseYear?.message} {...register("releaseYear")} />
        <TextField label={SAVE_VEHICLE_TEXT.color} hint={SAVE_VEHICLE_TEXT.colorHint} error={errors.color?.message} {...register("color")} />
      </div>
      <TextField label={SAVE_VEHICLE_TEXT.tags} hint={SAVE_VEHICLE_TEXT.tagsHint} error={errors.tags?.message} {...register("tags")} />
      <CheckboxField label={SAVE_VEHICLE_TEXT.electric} {...register("electric")} />
      <FormError message={errors.root?.server?.message} />
      {isNew ? (
        <ModalFooter>
          <Button variant="secondary" onClick={onDone} disabled={isPending}>
            {SAVE_VEHICLE_TEXT.cancel}
          </Button>
          <Button type="submit" isLoading={isPending}>
            {SAVE_VEHICLE_TEXT.create}
          </Button>
        </ModalFooter>
      ) : (
        <div className="flex justify-end">
          <Button type="submit" isLoading={isPending} disabled={!isDirty}>
            {SAVE_VEHICLE_TEXT.save}
          </Button>
        </div>
      )}
    </form>
  );
};
