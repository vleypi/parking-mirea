"use client";

import { useState } from "react";
import { Button, ConfirmModal } from "@/shared/ui";
import { type INote, NoteCard } from "@/entities/note";
import { NoteForm } from "@/features/manage-note/components/NoteForm";
import { MANAGE_NOTE_TEXT } from "@/features/manage-note/constants/manage-note.constant";
import { useNoteMutations } from "@/features/manage-note/hooks/useNoteMutations";

interface IEditableNoteProps {
  basePath: string;
  note: INote;
}

export const EditableNote = ({ basePath, note }: IEditableNoteProps) => {
  const [isEditing, setIsEditing] = useState(false);
  const [isRemoving, setIsRemoving] = useState(false);
  const { update, remove } = useNoteMutations(basePath);

  if (isEditing) {
    return (
      <NoteForm
        initialValues={{ body: note.body, pinned: note.pinned }}
        submitText={MANAGE_NOTE_TEXT.save}
        isPending={update.isPending}
        onSubmit={(payload) => update.mutateAsync({ note, payload }).then(() => setIsEditing(false))}
        onCancel={() => setIsEditing(false)}
      />
    );
  }

  const actions = note.canEdit && (
    <>
      <Button variant="ghost" className="h-8 px-3" onClick={() => setIsEditing(true)}>
        {MANAGE_NOTE_TEXT.edit}
      </Button>
      <Button
        variant="ghost"
        className="h-8 px-3"
        isLoading={update.isPending}
        onClick={() => update.mutate({ note, payload: { body: note.body, pinned: !note.pinned } })}
      >
        {note.pinned ? MANAGE_NOTE_TEXT.toggleUnpin : MANAGE_NOTE_TEXT.togglePin}
      </Button>
      <Button variant="danger" className="h-8 px-3" onClick={() => setIsRemoving(true)}>
        {MANAGE_NOTE_TEXT.remove}
      </Button>
    </>
  );

  return (
    <>
      <NoteCard note={note} actions={actions} />
      <ConfirmModal
        isOpen={isRemoving}
        title={MANAGE_NOTE_TEXT.removeTitle}
        description={MANAGE_NOTE_TEXT.removeDescription}
        confirmText={MANAGE_NOTE_TEXT.remove}
        isPending={remove.isPending}
        error={remove.error?.message}
        onConfirm={() => remove.mutate(note, { onSuccess: () => setIsRemoving(false) })}
        onClose={() => setIsRemoving(false)}
      />
    </>
  );
};
