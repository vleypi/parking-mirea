export type ImportMode = "CREATE_ONLY" | "UPSERT";

export interface IImportRowError {
  row: number;
  message: string;
}

/** Ответ POST .../import/preview: файл проверен, в базу ничего не записано. */
export interface IImportPreview {
  totalRows: number;
  validRows: number;
  errorRows: number;
  errors: IImportRowError[];
  importToken: string;
  expiresAt: string;
}

/** Ответ POST .../import/apply. */
export interface IImportResult {
  mode: ImportMode;
  created: number;
  updated: number;
  skipped: number;
  errors: IImportRowError[];
}
