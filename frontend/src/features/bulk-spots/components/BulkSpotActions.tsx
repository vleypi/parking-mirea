"use client";

import { useState } from "react";
import type { IBulkResult } from "@/shared/api";
import { joinClassNames } from "@/shared/lib";
import { BulkResult, Button, FormError, Modal, ModalFooter, SelectControl, TOOLBAR_CONTROL_CLASS_NAME } from "@/shared/ui";
import { SPOT_BULK_ACTIONS, SPOT_STATUS_LABELS, SPOT_STATUSES, type SpotBulkAction, type SpotStatus } from "@/entities/spot";
import { BULK_ACTION_OPTIONS, BULK_SPOTS_TEXT, TAG_BULK_ACTIONS } from "@/features/bulk-spots/constants/bulk-spots.constant";
import { useBulkSpots } from "@/features/bulk-spots/hooks/useBulkSpots";

interface IBulkSpotActionsProps {
  ids: string[];
  getLabel: (id: string) => string;
  onDone: () => void;
}

const STATUS_OPTIONS = Object.values(SPOT_STATUSES).map((status) => ({ value: status, label: SPOT_STATUS_LABELS[status] }));

export const BulkSpotActions = ({ ids, getLabel, onDone }: IBulkSpotActionsProps) => {
  const [action, setAction] = useState<SpotBulkAction>(SPOT_BULK_ACTIONS.archive);
  const [status, setStatus] = useState<SpotStatus>(SPOT_STATUSES.maintenance);
  const [tag, setTag] = useState("");
  const [result, setResult] = useState<IBulkResult | null>(null);
  const { mutate: apply, isPending, error } = useBulkSpots();
  const needsTag = TAG_BULK_ACTIONS.includes(action);

  const submit = () =>
    apply(
      {
        ids,
        action,
        status: action === SPOT_BULK_ACTIONS.changeStatus ? status : undefined,
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
        aria-label={BULK_SPOTS_TEXT.action}
        options={BULK_ACTION_OPTIONS}
        value={action}
        onChange={(event) => setAction(event.target.value as SpotBulkAction)}
        className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-auto")}
      />
      {action === SPOT_BULK_ACTIONS.changeStatus && (
        <SelectControl
          aria-label={BULK_SPOTS_TEXT.status}
          options={STATUS_OPTIONS}
          value={status}
          onChange={(event) => setStatus(event.target.value as SpotStatus)}
          className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-auto")}
        />
      )}
      {needsTag && (
        <input
          aria-label={BULK_SPOTS_TEXT.tag}
          placeholder={BULK_SPOTS_TEXT.tag}
          value={tag}
          onChange={(event) => setTag(event.target.value)}
          className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-32 px-3")}
        />
      )}
      <Button className="h-8 px-3" isLoading={isPending} disabled={needsTag && !tag.trim()} onClick={submit}>
        {BULK_SPOTS_TEXT.apply}
      </Button>
      <FormError message={error?.message} />
      <Modal isOpen={result !== null} title={BULK_SPOTS_TEXT.resultTitle} onClose={closeResult}>
        {result && <BulkResult result={result} getLabel={getLabel} />}
        <ModalFooter>
          <Button onClick={closeResult}>{BULK_SPOTS_TEXT.done}</Button>
        </ModalFooter>
      </Modal>
    </>
  );
};
