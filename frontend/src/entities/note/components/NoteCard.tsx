import type { ReactNode } from "react";
import { formatDateTime, joinClassNames } from "@/shared/lib";
import { Avatar, Badge } from "@/shared/ui";
import { NOTE_TEXT } from "@/entities/note/constants/note.constant";
import type { INote } from "@/entities/note/interfaces/note.interface";

interface INoteCardProps {
  note: INote;
  actions?: ReactNode;
}

export const NoteCard = ({ note, actions }: INoteCardProps) => (
  <article className={joinClassNames("rounded-xl border bg-white p-4", note.pinned ? "border-brand/30" : "border-gray-200")}>
    <header className="mb-2 flex items-center gap-2">
      <Avatar name={note.author.displayName} size="sm" />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-ink">{note.author.displayName}</p>
        <p className="text-xs text-gray-500">
          {formatDateTime(note.createdAt)}
          {note.updatedAt !== note.createdAt && `, ${NOTE_TEXT.edited} ${formatDateTime(note.updatedAt)}`}
        </p>
      </div>
      {note.pinned && <Badge tone="brand">{NOTE_TEXT.pinned}</Badge>}
    </header>
    <p className="text-sm whitespace-pre-wrap text-gray-700">{note.body}</p>
    {actions && <footer className="mt-3 flex flex-wrap gap-1">{actions}</footer>}
  </article>
);
