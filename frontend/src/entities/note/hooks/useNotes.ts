"use client";

import { useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { noteService } from "@/entities/note/services/note.service";

export const useNotes = (basePath: string) => useQuery({ queryKey: QUERY_KEYS.notes(basePath), queryFn: () => noteService.getAll(basePath) });
