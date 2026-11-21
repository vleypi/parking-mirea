"use client";

import { FormError } from "@/shared/ui";
import { OwnerDetails, OwnerRelatedList, useOwner } from "@/entities/owner";
import { ArchiveOwnerActions } from "@/features/archive-owner";
import { OwnerForm, SAVE_OWNER_TEXT } from "@/features/save-owner";
import { NotesPanel } from "@/widgets/notes-panel";
import { OWNER_CARD_TEXT, ownerNotesPath } from "@/widgets/owner-card/constants/owner-card.constant";

interface IOwnerCardProps {
  ownerId: string;
  canEdit: boolean;
  canRemovePermanently: boolean;
  onRemoved: () => void;
}

/** Карточка владельца: данные, машины и заявки, действия по роли и заметки. VIEWER видит всё без кнопок изменения. */
export const OwnerCard = ({ ownerId, canEdit, canRemovePermanently, onRemoved }: IOwnerCardProps) => {
  const { data: owner, isPending, isError } = useOwner(ownerId);

  if (isPending) return <div className="h-40 animate-pulse rounded-xl bg-gray-100" />;
  if (isError) return <FormError message={OWNER_CARD_TEXT.loadFailed} />;

  return (
    <div className="grid gap-8 pb-6 lg:grid-cols-[3fr_2fr]">
      <div className="flex flex-col gap-6">
        <OwnerDetails owner={owner} />
        {canEdit && <ArchiveOwnerActions owner={owner} canRemovePermanently={canRemovePermanently} onRemoved={onRemoved} />}
        {canEdit && !owner.archived && <OwnerForm owner={owner} />}
        {canEdit && owner.archived && <p className="text-sm text-gray-500">{SAVE_OWNER_TEXT.readOnly}</p>}
        <OwnerRelatedList ownerId={owner.id} />
      </div>
      <NotesPanel basePath={ownerNotesPath(ownerId)} canWrite={canEdit} />
    </div>
  );
};
