"use client";

import { FormError } from "@/shared/ui";
import { isRequestEditable, RequestDetails, useRequest } from "@/entities/request";
import { ArchiveRequestActions } from "@/features/archive-request";
import { EDIT_REQUEST_TEXT, RequestForm } from "@/features/edit-request";
import { NotesPanel } from "@/widgets/notes-panel";
import { REQUEST_CARD_TEXT, requestNotesPath } from "@/widgets/request-card/constants/request-card.constant";

interface IRequestCardProps {
  requestId: string;
  canEdit: boolean;
  canRemovePermanently: boolean;
  onRemoved: () => void;
}

export const RequestCard = ({ requestId, canEdit, canRemovePermanently, onRemoved }: IRequestCardProps) => {
  const { data: request, isPending, isError } = useRequest(requestId);

  if (isPending) return <div className="h-40 animate-pulse rounded-xl bg-gray-100" />;
  if (isError) return <FormError message={REQUEST_CARD_TEXT.loadFailed} />;

  return (
    <div className="grid gap-8 pb-6 lg:grid-cols-[3fr_2fr]">
      <div className="flex flex-col gap-6">
        <RequestDetails request={request} />
        {canEdit && <ArchiveRequestActions request={request} canRemovePermanently={canRemovePermanently} onRemoved={onRemoved} />}
        {canEdit && isRequestEditable(request) && <RequestForm request={request} />}
        {canEdit && !isRequestEditable(request) && <p className="text-sm text-gray-500">{EDIT_REQUEST_TEXT.readOnly}</p>}
      </div>
      <NotesPanel basePath={requestNotesPath(requestId)} canWrite={canEdit} />
    </div>
  );
};
