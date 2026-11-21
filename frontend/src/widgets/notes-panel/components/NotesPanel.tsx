"use client";

import { FormError, SectionHeader } from "@/shared/ui";
import { useNotes } from "@/entities/note";
import { AddNoteForm, EditableNote } from "@/features/manage-note";
import { NOTES_PANEL_TEXT } from "@/widgets/notes-panel/constants/notes-panel.constant";

interface INotesPanelProps {
  basePath: string;
  canWrite: boolean;
}

export const NotesPanel = ({ basePath, canWrite }: INotesPanelProps) => {
  const { data: notes, isPending, error } = useNotes(basePath);

  return (
    <section className="flex flex-col gap-3">
      <SectionHeader title={NOTES_PANEL_TEXT.title} description={NOTES_PANEL_TEXT.description} />
      {canWrite && <AddNoteForm basePath={basePath} />}
      <FormError message={error ? NOTES_PANEL_TEXT.loadFailed : undefined} />
      {!isPending && notes?.length === 0 && <p className="text-sm text-gray-500">{NOTES_PANEL_TEXT.empty}</p>}
      {notes?.map((note) => <EditableNote key={note.id} basePath={basePath} note={note} />)}
    </section>
  );
};
