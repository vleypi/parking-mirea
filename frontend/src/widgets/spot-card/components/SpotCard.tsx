"use client";

import { FormError } from "@/shared/ui";
import { SpotDetails, SpotRequestList, useSpot } from "@/entities/spot";
import { ArchiveSpotActions } from "@/features/archive-spot";
import { SAVE_SPOT_TEXT, SpotForm } from "@/features/save-spot";
import { NotesPanel } from "@/widgets/notes-panel";
import { SPOT_CARD_TEXT, spotNotesPath } from "@/widgets/spot-card/constants/spot-card.constant";

interface ISpotCardProps {
  spotId: string;
  canEdit: boolean;
  canRemovePermanently: boolean;
  onRemoved: () => void;
}

/** Карточка места: данные и доступность сейчас, заявки, действия по роли и заметки. VIEWER видит всё без кнопок изменения. */
export const SpotCard = ({ spotId, canEdit, canRemovePermanently, onRemoved }: ISpotCardProps) => {
  const { data: spot, isPending, isError } = useSpot(spotId);

  if (isPending) return <div className="h-40 animate-pulse rounded-xl bg-gray-100" />;
  if (isError) return <FormError message={SPOT_CARD_TEXT.loadFailed} />;

  return (
    <div className="grid gap-8 pb-6 lg:grid-cols-[3fr_2fr]">
      <div className="flex flex-col gap-6">
        <SpotDetails spot={spot} />
        {canEdit && <ArchiveSpotActions spot={spot} canRemovePermanently={canRemovePermanently} onRemoved={onRemoved} />}
        {canEdit && !spot.archived && <SpotForm spot={spot} />}
        {canEdit && spot.archived && <p className="text-sm text-gray-500">{SAVE_SPOT_TEXT.readOnly}</p>}
        <SpotRequestList spotId={spot.id} />
      </div>
      <NotesPanel basePath={spotNotesPath(spotId)} canWrite={canEdit} />
    </div>
  );
};
