"use client";

import { NoteForm } from "@/features/manage-note/components/NoteForm";
import { MANAGE_NOTE_TEXT } from "@/features/manage-note/constants/manage-note.constant";
import { useNoteMutations } from "@/features/manage-note/hooks/useNoteMutations";

interface IAddNoteFormProps {
  basePath: string;
}

export const AddNoteForm = ({ basePath }: IAddNoteFormProps) => {
  const { create } = useNoteMutations(basePath);

  return <NoteForm submitText={MANAGE_NOTE_TEXT.add} isPending={create.isPending} onSubmit={create.mutateAsync} />;
};
