"use client";

import { useState } from "react";
import type { IImportRowError, ImportMode } from "@/shared/api";
import { Button, FormError, Modal, ModalFooter, SelectField } from "@/shared/ui";
import { CSV_TRANSFER_TEXT, IMPORT_MODE_OPTIONS } from "@/features/csv-transfer/constants/csv-transfer.constant";
import { useCsvTransfer } from "@/features/csv-transfer/hooks/useCsvTransfer";

interface ICsvTransferActionsProps {
  basePath: string;
  invalidateKey: readonly string[];
  exportParams: Record<string, string | number | undefined>;
  fileName: string;
  canImport: boolean;
}

interface IRowErrorsProps {
  errors: IImportRowError[];
}

const RowErrors = ({ errors }: IRowErrorsProps) =>
  errors.length > 0 ? (
    <ul className="flex max-h-64 flex-col gap-1.5 overflow-y-auto">
      {errors.map(({ row, message }) => (
        <li key={`${row}-${message}`} className="rounded-lg bg-red-50 px-3 py-2 text-sm">
          <span className="font-medium text-ink">
            {CSV_TRANSFER_TEXT.row} {row}
          </span>
          <span className="text-red-700">: {message}</span>
        </li>
      ))}
    </ul>
  ) : null;

/** Экспорт текущего фильтра и импорт в три шага: выбор файла → предпросмотр ошибок → применение. */
export const CsvTransferActions = ({ basePath, invalidateKey, exportParams, fileName, canImport }: ICsvTransferActionsProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [mode, setMode] = useState<ImportMode>("CREATE_ONLY");
  const { exportFile, preview, apply } = useCsvTransfer(basePath, invalidateKey);
  const checked = preview.data;
  const result = apply.data;

  const reset = () => {
    setFile(null);
    preview.reset();
    apply.reset();
  };

  const close = () => {
    reset();
    setIsOpen(false);
  };

  return (
    <>
      <Button variant="secondary" isLoading={exportFile.isPending} onClick={() => exportFile.mutate({ params: exportParams, fallbackName: fileName })}>
        {CSV_TRANSFER_TEXT.export}
      </Button>
      {canImport && (
        <Button variant="secondary" onClick={() => setIsOpen(true)}>
          {CSV_TRANSFER_TEXT.import}
        </Button>
      )}
      {exportFile.isError && <FormError message={exportFile.error.message || CSV_TRANSFER_TEXT.exportFailed} />}
      <Modal size="lg" isOpen={isOpen} title={CSV_TRANSFER_TEXT.importTitle} onClose={close}>
        <div className="flex flex-col gap-4 pb-2 text-sm">
          {!checked && (
            <label className="flex flex-col gap-1.5">
              <span className="font-medium text-ink">{CSV_TRANSFER_TEXT.file}</span>
              <input type="file" accept=".csv,text/csv" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
              <span className="text-xs text-gray-500">{CSV_TRANSFER_TEXT.fileHint}</span>
            </label>
          )}
          {checked && !result && (
            <>
              <p className="text-gray-600">{CSV_TRANSFER_TEXT.previewNote}</p>
              <p className="tabular-nums">
                {CSV_TRANSFER_TEXT.total}: {checked.totalRows} · <span className="text-green-700">{CSV_TRANSFER_TEXT.valid}: {checked.validRows}</span> ·{" "}
                <span className="text-red-700">
                  {CSV_TRANSFER_TEXT.invalid}: {checked.errorRows}
                </span>
              </p>
              <RowErrors errors={checked.errors} />
              {checked.validRows > 0 ? (
                <SelectField
                  label={CSV_TRANSFER_TEXT.mode}
                  options={IMPORT_MODE_OPTIONS}
                  value={mode}
                  onChange={(event) => setMode(event.target.value as ImportMode)}
                />
              ) : (
                <p className="text-gray-600">{CSV_TRANSFER_TEXT.nothingToApply}</p>
              )}
            </>
          )}
          {result && (
            <>
              <p className="tabular-nums">
                {CSV_TRANSFER_TEXT.created}: {result.created} · {CSV_TRANSFER_TEXT.updated}: {result.updated} · {CSV_TRANSFER_TEXT.skipped}:{" "}
                {result.skipped}
              </p>
              {result.errors.length > 0 && <p className="text-red-700">{CSV_TRANSFER_TEXT.applyErrors}:</p>}
              <RowErrors errors={result.errors} />
            </>
          )}
          <FormError message={preview.error?.message ?? apply.error?.message} />
        </div>
        <ModalFooter>
          {result ? (
            <Button onClick={close}>{CSV_TRANSFER_TEXT.done}</Button>
          ) : (
            <>
              <Button variant="secondary" onClick={checked ? reset : close} disabled={apply.isPending}>
                {checked ? CSV_TRANSFER_TEXT.checkAgain : CSV_TRANSFER_TEXT.cancel}
              </Button>
              {checked ? (
                <Button
                  isLoading={apply.isPending}
                  disabled={checked.validRows === 0}
                  onClick={() => apply.mutate({ importToken: checked.importToken, mode })}
                >
                  {CSV_TRANSFER_TEXT.apply}
                </Button>
              ) : (
                <Button isLoading={preview.isPending} disabled={!file} onClick={() => file && preview.mutate(file)}>
                  {CSV_TRANSFER_TEXT.check}
                </Button>
              )}
            </>
          )}
        </ModalFooter>
      </Modal>
    </>
  );
};
