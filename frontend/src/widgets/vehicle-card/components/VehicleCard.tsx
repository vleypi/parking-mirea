"use client";

import { FormError } from "@/shared/ui";
import { useVehicle, VehicleDetails, VehicleRequestList } from "@/entities/vehicle";
import { ArchiveVehicleActions } from "@/features/archive-vehicle";
import { SAVE_VEHICLE_TEXT, VehicleForm } from "@/features/save-vehicle";
import { NotesPanel } from "@/widgets/notes-panel";
import { VEHICLE_CARD_TEXT, vehicleNotesPath } from "@/widgets/vehicle-card/constants/vehicle-card.constant";

interface IVehicleCardProps {
  vehicleId: string;
  canEdit: boolean;
  canRemovePermanently: boolean;
  onRemoved: () => void;
}

/** Карточка автомобиля: данные, заявки, действия по роли и заметки. VIEWER видит всё без кнопок изменения. */
export const VehicleCard = ({ vehicleId, canEdit, canRemovePermanently, onRemoved }: IVehicleCardProps) => {
  const { data: vehicle, isPending, isError } = useVehicle(vehicleId);

  if (isPending) return <div className="h-40 animate-pulse rounded-xl bg-gray-100" />;
  if (isError) return <FormError message={VEHICLE_CARD_TEXT.loadFailed} />;

  return (
    <div className="grid gap-8 pb-6 lg:grid-cols-[3fr_2fr]">
      <div className="flex flex-col gap-6">
        <VehicleDetails vehicle={vehicle} />
        {canEdit && <ArchiveVehicleActions vehicle={vehicle} canRemovePermanently={canRemovePermanently} onRemoved={onRemoved} />}
        {canEdit && !vehicle.archived && <VehicleForm vehicle={vehicle} />}
        {canEdit && vehicle.archived && <p className="text-sm text-gray-500">{SAVE_VEHICLE_TEXT.readOnly}</p>}
        <VehicleRequestList vehicleId={vehicle.id} />
      </div>
      <NotesPanel basePath={vehicleNotesPath(vehicleId)} canWrite={canEdit} />
    </div>
  );
};
