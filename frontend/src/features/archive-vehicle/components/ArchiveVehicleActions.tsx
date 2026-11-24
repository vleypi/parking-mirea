"use client";

import { useState } from "react";
import { Button, ConfirmModal, FormError } from "@/shared/ui";
import type { IVehicle } from "@/entities/vehicle";
import { ARCHIVE_VEHICLE_TEXT } from "@/features/archive-vehicle/constants/archive-vehicle.constant";
import { useArchiveVehicle } from "@/features/archive-vehicle/hooks/useArchiveVehicle";

interface IArchiveVehicleActionsProps {
  vehicle: IVehicle;
  canRemovePermanently: boolean;
  onRemoved: () => void;
}

export const ArchiveVehicleActions = ({ vehicle, canRemovePermanently, onRemoved }: IArchiveVehicleActionsProps) => {
  const [isRemoving, setIsRemoving] = useState(false);
  const { toggleArchive, removePermanently } = useArchiveVehicle(vehicle);

  return (
    <div className="flex flex-col items-end gap-2">
      <div className="flex flex-wrap gap-2">
        <Button variant="secondary" isLoading={toggleArchive.isPending} onClick={() => toggleArchive.mutate()}>
          {vehicle.archived ? ARCHIVE_VEHICLE_TEXT.restore : ARCHIVE_VEHICLE_TEXT.archive}
        </Button>
        {canRemovePermanently && (
          <Button variant="danger" onClick={() => setIsRemoving(true)}>
            {ARCHIVE_VEHICLE_TEXT.remove}
          </Button>
        )}
      </div>
      <FormError message={toggleArchive.error?.message} />
      <ConfirmModal
        isOpen={isRemoving}
        title={ARCHIVE_VEHICLE_TEXT.removeTitle}
        description={ARCHIVE_VEHICLE_TEXT.removeDescription}
        confirmText={ARCHIVE_VEHICLE_TEXT.remove}
        isPending={removePermanently.isPending}
        error={removePermanently.error?.message}
        onConfirm={() => removePermanently.mutate(undefined, { onSuccess: onRemoved })}
        onClose={() => setIsRemoving(false)}
      />
    </div>
  );
};
