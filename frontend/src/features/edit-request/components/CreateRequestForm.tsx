"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { applyServerErrors, formatMoney } from "@/shared/lib";
import { Button, CheckboxField, FormError, ModalFooter, TextAreaField, TextField } from "@/shared/ui";
import { type IOwnerOption, OwnerPicker } from "@/entities/owner";
import { useFreeSpots } from "@/entities/spot";
import { SpotSelect } from "@/features/edit-request/components/SpotSelect";
import { VehicleSelect } from "@/features/edit-request/components/VehicleSelect";
import { EDIT_REQUEST_TEXT } from "@/features/edit-request/constants/edit-request.constant";
import { useCreateRequest } from "@/features/edit-request/hooks/useCreateRequest";
import { type CreateRequestFormValues, createRequestFormSchema } from "@/features/edit-request/schemas/create-request-form.schema";
import { EMPTY_CREATE_FORM, estimateCost, toPeriod } from "@/features/edit-request/utils/request-form.util";

interface ICreateRequestFormProps {
  onDone: () => void;
}

/** Новая заявка: владелец поиском, затем его автомобиль, период и свободное на этот период место. */
export const CreateRequestForm = ({ onDone }: ICreateRequestFormProps) => {
  const [owner, setOwner] = useState<IOwnerOption | null>(null);
  const { mutate: create, isPending } = useCreateRequest();
  const {
    register,
    handleSubmit,
    setError,
    setValue,
    control,
    formState: { errors },
  } = useForm<CreateRequestFormValues>({ resolver: zodResolver(createRequestFormSchema), defaultValues: EMPTY_CREATE_FORM });
  const [startTime, endTime, spotId] = useWatch({ control, name: ["startTime", "endTime", "spotId"] });
  const period = toPeriod(startTime, endTime);
  const { data: freeSpots } = useFreeSpots(period.from, period.to);
  const spot = freeSpots?.find(({ id }) => id === spotId);

  const selectOwner = (next: IOwnerOption) => {
    setOwner(next);
    setValue("ownerId", next.id, { shouldValidate: true });
    setValue("vehicleId", "");
  };

  const onSubmit = (values: CreateRequestFormValues) => create(values, { onSuccess: onDone, onError: (error) => applyServerErrors(error, setError) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
      <OwnerPicker selected={owner} error={errors.ownerId?.message} onSelect={selectOwner} />
      <VehicleSelect ownerId={owner?.id ?? ""} error={errors.vehicleId?.message} {...register("vehicleId")} />
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label={EDIT_REQUEST_TEXT.startTime} type="datetime-local" error={errors.startTime?.message} {...register("startTime")} />
        <TextField label={EDIT_REQUEST_TEXT.endTime} type="datetime-local" error={errors.endTime?.message} {...register("endTime")} />
      </div>
      <SpotSelect from={startTime} to={endTime} error={errors.spotId?.message} {...register("spotId")} />
      {spot && (
        <p className="text-sm text-gray-600">
          {EDIT_REQUEST_TEXT.estimatedCost}: <span className="font-medium text-ink">{formatMoney(estimateCost(spot.hourlyRate, startTime, endTime))}</span>
        </p>
      )}
      <TextField label={EDIT_REQUEST_TEXT.tags} hint={EDIT_REQUEST_TEXT.tagsHint} error={errors.tags?.message} {...register("tags")} />
      <TextAreaField label={EDIT_REQUEST_TEXT.description} error={errors.description?.message} {...register("description")} />
      <CheckboxField label={EDIT_REQUEST_TEXT.paid} {...register("paid")} />
      <FormError message={errors.root?.server?.message} />
      <ModalFooter>
        <Button variant="secondary" onClick={onDone} disabled={isPending}>
          {EDIT_REQUEST_TEXT.cancel}
        </Button>
        <Button type="submit" isLoading={isPending}>
          {EDIT_REQUEST_TEXT.create}
        </Button>
      </ModalFooter>
    </form>
  );
};
