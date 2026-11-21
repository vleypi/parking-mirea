"use client";

import { useState } from "react";
import { Button, ConfirmModal, FormError } from "@/shared/ui";
import type { IRequest } from "@/entities/request";
import { ARCHIVE_REQUEST_TEXT } from "@/features/archive-request/constants/archive-request.constant";
import { useArchiveRequest } from "@/features/archive-request/hooks/useArchiveRequest";

interface IArchiveRequestActionsProps {
  request: IRequest;
  canRemovePermanently: boolean;
  onRemoved: () => void;
}

export const ArchiveRequestActions = ({ request, canRemovePermanently, onRemoved }: IArchiveRequestActionsProps) => {
  const [isRemoving, setIsRemoving] = useState(false);
  const { toggleArchive, removePermanently } = useArchiveRequest(request);

  return (
    <div className="flex flex-col items-end gap-2">
      <div className="flex flex-wrap gap-2">
        <Button variant="secondary" isLoading={toggleArchive.isPending} onClick={() => toggleArchive.mutate()}>
          {request.archived ? ARCHIVE_REQUEST_TEXT.restore : ARCHIVE_REQUEST_TEXT.archive}
        </Button>
        {canRemovePermanently && (
          <Button variant="danger" onClick={() => setIsRemoving(true)}>
            {ARCHIVE_REQUEST_TEXT.remove}
          </Button>
        )}
      </div>
      <FormError message={toggleArchive.error?.message} />
      <ConfirmModal
        isOpen={isRemoving}
        title={ARCHIVE_REQUEST_TEXT.removeTitle}
        description={ARCHIVE_REQUEST_TEXT.removeDescription}
        confirmText={ARCHIVE_REQUEST_TEXT.remove}
        isPending={removePermanently.isPending}
        error={removePermanently.error?.message}
        onConfirm={() => removePermanently.mutate(undefined, { onSuccess: onRemoved })}
        onClose={() => setIsRemoving(false)}
      />
    </div>
  );
};
