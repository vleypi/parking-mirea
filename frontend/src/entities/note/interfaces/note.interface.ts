export interface INoteAuthor {
  id: string;
  displayName: string;
}

export interface INote {
  id: string;
  body: string;
  pinned: boolean;
  author: INoteAuthor;
  createdAt: string;
  updatedAt: string;
  version: number;
  canEdit: boolean;
}

export interface INotePayload {
  body: string;
  pinned: boolean;
}

export interface INoteUpdatePayload extends INotePayload {
  version: number;
}
