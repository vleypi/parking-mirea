"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type INote, type INotePayload, noteService } from "@/entities/note";

export const useNoteMutations = (basePath: string) => {
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.notes(basePath) });

  return {
    create: useMutation({ mutationFn: (payload: INotePayload) => noteService.create(basePath, payload), onSuccess }),
    update: useMutation({
      mutationFn: ({ note, payload }: { note: INote; payload: INotePayload }) =>
        noteService.update(basePath, note.id, { ...payload, version: note.version }),
      onSuccess,
    }),
    remove: useMutation({ mutationFn: (note: INote) => noteService.remove(basePath, note.id), onSuccess }),
  };
};
