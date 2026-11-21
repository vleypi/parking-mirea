"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyServerErrors } from "@/shared/lib";
import { Button, CheckboxField, FormError, SelectField, TextAreaField, TextField } from "@/shared/ui";
import { type IRequest, REQUEST_STATUS_LABELS, REQUEST_STATUS_TRANSITIONS } from "@/entities/request";
import { EDIT_REQUEST_TEXT } from "@/features/edit-request/constants/edit-request.constant";
import { useUpdateRequest } from "@/features/edit-request/hooks/useUpdateRequest";
import { requestFormSchema, type RequestFormValues } from "@/features/edit-request/schemas/request-form.schema";
import { toRequestFormValues } from "@/features/edit-request/utils/request-form.util";

interface IRequestFormProps {
  request: IRequest;
}

export const RequestForm = ({ request }: IRequestFormProps) => {
  const { mutate: update, isPending } = useUpdateRequest(request);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isDirty },
  } = useForm<RequestFormValues>({ resolver: zodResolver(requestFormSchema), values: toRequestFormValues(request) });
  const statusOptions = REQUEST_STATUS_TRANSITIONS[request.status.code].map((status) => ({ value: status, label: REQUEST_STATUS_LABELS[status] }));

  const onSubmit = (values: RequestFormValues) => update(values, { onError: (error) => applyServerErrors(error, setError) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
      <div className="grid gap-4 sm:grid-cols-3">
        <SelectField label={EDIT_REQUEST_TEXT.status} options={statusOptions} error={errors.status?.message} {...register("status")} />
        <TextField label={EDIT_REQUEST_TEXT.startTime} type="datetime-local" error={errors.startTime?.message} {...register("startTime")} />
        <TextField label={EDIT_REQUEST_TEXT.endTime} type="datetime-local" error={errors.endTime?.message} {...register("endTime")} />
      </div>
      <TextField label={EDIT_REQUEST_TEXT.tags} hint={EDIT_REQUEST_TEXT.tagsHint} error={errors.tags?.message} {...register("tags")} />
      <TextAreaField label={EDIT_REQUEST_TEXT.description} error={errors.description?.message} {...register("description")} />
      <CheckboxField label={EDIT_REQUEST_TEXT.paid} {...register("paid")} />
      <FormError message={errors.root?.server?.message} />
      <div className="flex justify-end">
        <Button type="submit" isLoading={isPending} disabled={!isDirty}>
          {EDIT_REQUEST_TEXT.save}
        </Button>
      </div>
    </form>
  );
};
