import { apiClient, type IImportPreview, type IImportResult, type ImportMode } from "@/shared/api";

const CSV_TIMEOUT_MS = 60_000;
const FILENAME_PATTERN = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i;

/** Общий CSV-обмен для сущностей: basePath — "/vehicles" или "/spots". */
class CsvTransferService {
  async download(basePath: string, params: Record<string, string | number | undefined>, fallbackName: string): Promise<void> {
    const response = await apiClient.get<Blob>(`${basePath}/export`, { params, responseType: "blob", timeout: CSV_TIMEOUT_MS });
    const disposition = String(response.headers["content-disposition"] ?? "");
    const name = decodeURIComponent(FILENAME_PATTERN.exec(disposition)?.[1] ?? fallbackName);
    const url = URL.createObjectURL(response.data);
    const link = document.createElement("a");
    link.href = url;
    link.download = name;
    link.click();
    URL.revokeObjectURL(url);
  }

  async preview(basePath: string, file: File): Promise<IImportPreview> {
    const form = new FormData();
    form.append("file", file);
    const { data } = await apiClient.post<IImportPreview>(`${basePath}/import/preview`, form, { timeout: CSV_TIMEOUT_MS });
    return data;
  }

  async apply(basePath: string, importToken: string, mode: ImportMode): Promise<IImportResult> {
    const { data } = await apiClient.post<IImportResult>(`${basePath}/import/apply`, { importToken, mode }, { timeout: CSV_TIMEOUT_MS });
    return data;
  }
}

export const csvTransferService = new CsvTransferService();
