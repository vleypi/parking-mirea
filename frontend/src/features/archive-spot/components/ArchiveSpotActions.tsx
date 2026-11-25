"use client";

import { useState } from "react";
import { Button, ConfirmModal, FormError } from "@/shared/ui";
import type { ISpot } from "@/entities/spot";
import { ARCHIVE_SPOT_TEXT } from "@/features/archive-spot/constants/archive-spot.constant";
import { useArchiveSpot } from "@/features/archive-spot/hooks/useArchiveSpot";

interface IArchiveSpotActionsProps {
  spot: ISpot;
  canRemovePermanently: boolean;
  onRemoved: () => void;
}

export const ArchiveSpotActions = ({ spot, canRemovePermanently, onRemoved }: IArchiveSpotActionsProps) => {
  const [isRemoving, setIsRemoving] = useState(false);
  const { toggleArchive, removePermanently } = useArchiveSpot(spot);

  return (
    <div className="flex flex-col items-end gap-2">
      <div className="flex flex-wrap gap-2">
        <Button variant="secondary" isLoading={toggleArchive.isPending} onClick={() => toggleArchive.mutate()}>
          {spot.archived ? ARCHIVE_SPOT_TEXT.restore : ARCHIVE_SPOT_TEXT.archive}
        </Button>
        {canRemovePermanently && (
          <Button variant="danger" onClick={() => setIsRemoving(true)}>
            {ARCHIVE_SPOT_TEXT.remove}
          </Button>
        )}
      </div>
      <FormError message={toggleArchive.error?.message} />
      <ConfirmModal
        isOpen={isRemoving}
        title={ARCHIVE_SPOT_TEXT.removeTitle}
        description={ARCHIVE_SPOT_TEXT.removeDescription}
        confirmText={ARCHIVE_SPOT_TEXT.remove}
        isPending={removePermanently.isPending}
        error={removePermanently.error?.message}
        onConfirm={() => removePermanently.mutate(undefined, { onSuccess: onRemoved })}
        onClose={() => setIsRemoving(false)}
      />
    </div>
  );
};
