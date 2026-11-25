"use client";

import { useState } from "react";
import type { IBulkResult } from "@/shared/api";
import { joinClassNames } from "@/shared/lib";
import { BulkResult, Button, FormError, Modal, ModalFooter, SelectControl, TOOLBAR_CONTROL_CLASS_NAME } from "@/shared/ui";
import { OWNER_BULK_ACTIONS, OWNER_STATUS_LABELS, OWNER_STATUSES, type OwnerBulkAction, type OwnerStatus } from "@/entities/owner";
import { BULK_ACTION_OPTIONS, BULK_OWNERS_TEXT, TAG_BULK_ACTIONS } from "@/features/bulk-owners/constants/bulk-owners.constant";
import { useBulkOwners } from "@/features/bulk-owners/hooks/useBulkOwners";

interface IBulkOwnerActionsProps {
  ids: string[];
  getLabel: (id: string) => string;
  onDone: () => void;
}

const STATUS_OPTIONS = Object.values(OWNER_STATUSES).map((status) => ({ value: status, label: OWNER_STATUS_LABELS[status] }));

export const BulkOwnerActions = ({ ids, getLabel, onDone }: IBulkOwnerActionsProps) => {
  const [action, setAction] = useState<OwnerBulkAction>(OWNER_BULK_ACTIONS.archive);
  const [status, setStatus] = useState<OwnerStatus>(OWNER_STATUSES.blocked);
  const [tag, setTag] = useState("");
  const [result, setResult] = useState<IBulkResult | null>(null);
  const { mutate: apply, isPending, error } = useBulkOwners();
  const needsTag = TAG_BULK_ACTIONS.includes(action);

  const submit = () =>
    apply(
      {
        ids,
        action,
        status: action === OWNER_BULK_ACTIONS.changeStatus ? status : undefined,
        tag: needsTag ? tag.trim() : undefined,
      },
      { onSuccess: setResult },
    );

  const closeResult = () => {
    setResult(null);
    onDone();
  };

  return (
    <>
      <SelectControl
        aria-label={BULK_OWNERS_TEXT.action}
        options={BULK_ACTION_OPTIONS}
        value={action}
        onChange={(event) => setAction(event.target.value as OwnerBulkAction)}
        className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-auto")}
      />
      {action === OWNER_BULK_ACTIONS.changeStatus && (
        <SelectControl
          aria-label={BULK_OWNERS_TEXT.status}
          options={STATUS_OPTIONS}
          value={status}
          onChange={(event) => setStatus(event.target.value as OwnerStatus)}
          className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-auto")}
        />
      )}
      {needsTag && (
        <input
          aria-label={BULK_OWNERS_TEXT.tag}
          placeholder={BULK_OWNERS_TEXT.tag}
          value={tag}
          onChange={(event) => setTag(event.target.value)}
          className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-32 px-3")}
        />
      )}
      <Button className="h-8 px-3" isLoading={isPending} disabled={needsTag && !tag.trim()} onClick={submit}>
        {BULK_OWNERS_TEXT.apply}
      </Button>
      <FormError message={error?.message} />
      <Modal isOpen={result !== null} title={BULK_OWNERS_TEXT.resultTitle} onClose={closeResult}>
        {result && <BulkResult result={result} getLabel={getLabel} />}
        <ModalFooter>
          <Button onClick={closeResult}>{BULK_OWNERS_TEXT.done}</Button>
        </ModalFooter>
      </Modal>
    </>
  );
};
