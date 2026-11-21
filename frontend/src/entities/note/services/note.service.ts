import { apiClient } from "@/shared/api";
import type { INote, INotePayload, INoteUpdatePayload } from "@/entities/note/interfaces/note.interface";

class NoteService {
  async getAll(basePath: string): Promise<INote[]> {
    const { data } = await apiClient.get<INote[]>(basePath);
    return data;
  }

  async create(basePath: string, payload: INotePayload): Promise<INote> {
    const { data } = await apiClient.post<INote>(basePath, payload);
    return data;
  }

  async update(basePath: string, noteId: string, payload: INoteUpdatePayload): Promise<INote> {
    const { data } = await apiClient.put<INote>(`${basePath}/${noteId}`, payload);
    return data;
  }

  async remove(basePath: string, noteId: string): Promise<void> {
    await apiClient.delete(`${basePath}/${noteId}`);
  }
}

export const noteService = new NoteService();
