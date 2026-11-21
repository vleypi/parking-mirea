import { z } from "zod";
import { MANAGE_NOTE_TEXT, NOTE_BODY_MAX_LENGTH } from "@/features/manage-note/constants/manage-note.constant";

export const noteFormSchema = z.object({
  body: z.string().trim().min(1, MANAGE_NOTE_TEXT.required).max(NOTE_BODY_MAX_LENGTH, MANAGE_NOTE_TEXT.tooLong),
  pinned: z.boolean(),
});

export type NoteFormValues = z.infer<typeof noteFormSchema>;
