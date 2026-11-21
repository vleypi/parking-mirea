"use client";

import { useState } from "react";
import { Button, ConfirmModal, FormError } from "@/shared/ui";
import type { IOwner } from "@/entities/owner";
import { ARCHIVE_OWNER_TEXT } from "@/features/archive-owner/constants/archive-owner.constant";
import { useArchiveOwner } from "@/features/archive-owner/hooks/useArchiveOwner";

interface IArchiveOwnerActionsProps {
  owner: IOwner;
  canRemovePermanently: boolean;
  onRemoved: () => void;
}

export const ArchiveOwnerActions = ({ owner, canRemovePermanently, onRemoved }: IArchiveOwnerActionsProps) => {
  const [isRemoving, setIsRemoving] = useState(false);
  const { toggleArchive, removePermanently } = useArchiveOwner(owner);

  return (
    <div className="flex flex-col items-end gap-2">
      <div className="flex flex-wrap gap-2">
        <Button variant="secondary" isLoading={toggleArchive.isPending} onClick={() => toggleArchive.mutate()}>
          {owner.archived ? ARCHIVE_OWNER_TEXT.restore : ARCHIVE_OWNER_TEXT.archive}
        </Button>
        {canRemovePermanently && (
          <Button variant="danger" onClick={() => setIsRemoving(true)}>
            {ARCHIVE_OWNER_TEXT.remove}
          </Button>
        )}
      </div>
      <FormError message={toggleArchive.error?.message} />
      <ConfirmModal
        isOpen={isRemoving}
        title={ARCHIVE_OWNER_TEXT.removeTitle}
        description={ARCHIVE_OWNER_TEXT.removeDescription}
        confirmText={ARCHIVE_OWNER_TEXT.remove}
        isPending={removePermanently.isPending}
        error={removePermanently.error?.message}
        onConfirm={() => removePermanently.mutate(undefined, { onSuccess: onRemoved })}
        onClose={() => setIsRemoving(false)}
      />
    </div>
  );
};
