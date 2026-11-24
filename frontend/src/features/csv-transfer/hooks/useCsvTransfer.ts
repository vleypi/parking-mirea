"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { ImportMode } from "@/shared/api";
import { csvTransferService } from "@/features/csv-transfer/services/csv-transfer.service";

/** Экспорт, проверка и применение CSV; после применения обновляются списки по invalidateKey. */
export const useCsvTransfer = (basePath: string, invalidateKey: readonly string[]) => {
  const queryClient = useQueryClient();

  return {
    exportFile: useMutation({
      mutationFn: ({ params, fallbackName }: { params: Record<string, string | number | undefined>; fallbackName: string }) =>
        csvTransferService.download(basePath, params, fallbackName),
    }),
    preview: useMutation({ mutationFn: (file: File) => csvTransferService.preview(basePath, file) }),
    apply: useMutation({
      mutationFn: ({ importToken, mode }: { importToken: string; mode: ImportMode }) => csvTransferService.apply(basePath, importToken, mode),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: invalidateKey }),
    }),
  };
};
