"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyServerErrors } from "@/shared/lib";
import { Button, CheckboxField, FormError, TextAreaField } from "@/shared/ui";
import { MANAGE_NOTE_TEXT } from "@/features/manage-note/constants/manage-note.constant";
import { noteFormSchema, type NoteFormValues } from "@/features/manage-note/schemas/note-form.schema";

interface INoteFormProps {
  initialValues?: NoteFormValues;
  submitText: string;
  isPending: boolean;
  onSubmit: (values: NoteFormValues) => Promise<unknown>;
  onCancel?: () => void;
}

const EMPTY_NOTE: NoteFormValues = { body: "", pinned: false };

export const NoteForm = ({ initialValues = EMPTY_NOTE, submitText, isPending, onSubmit, onCancel }: INoteFormProps) => {
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<NoteFormValues>({ resolver: zodResolver(noteFormSchema), defaultValues: initialValues });

  const submit = async (values: NoteFormValues) => {
    try {
      await onSubmit(values);
      reset(onCancel ? values : EMPTY_NOTE);
    } catch (error) {
      applyServerErrors(error, setError);
    }
  };

  return (
    <form onSubmit={handleSubmit(submit)} className="flex flex-col gap-2" noValidate>
      <TextAreaField label={MANAGE_NOTE_TEXT.placeholder} error={errors.body?.message} {...register("body")} />
      <CheckboxField label={MANAGE_NOTE_TEXT.pin} {...register("pinned")} />
      <FormError message={errors.root?.server?.message} />
      <div className="flex justify-end gap-2">
        {onCancel && (
          <Button variant="secondary" onClick={onCancel} disabled={isPending}>
            {MANAGE_NOTE_TEXT.cancel}
          </Button>
        )}
        <Button type="submit" isLoading={isPending}>
          {submitText}
        </Button>
      </div>
    </form>
  );
};
