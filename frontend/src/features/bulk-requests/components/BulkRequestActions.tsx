"use client";

import { useState } from "react";
import type { IBulkResult } from "@/shared/api";
import { joinClassNames } from "@/shared/lib";
import { BulkResult, Button, FormError, Modal, ModalFooter, SelectControl, TOOLBAR_CONTROL_CLASS_NAME } from "@/shared/ui";
import { REQUEST_BULK_ACTIONS, REQUEST_STATUS_LABELS, REQUEST_STATUSES, type RequestBulkAction, type RequestStatus } from "@/entities/request";
import { BULK_ACTION_OPTIONS, BULK_REQUESTS_TEXT, TAG_BULK_ACTIONS } from "@/features/bulk-requests/constants/bulk-requests.constant";
import { useBulkRequests } from "@/features/bulk-requests/hooks/useBulkRequests";

interface IBulkRequestActionsProps {
  ids: string[];
  getLabel: (id: string) => string;
  onDone: () => void;
}

const STATUS_OPTIONS = Object.values(REQUEST_STATUSES).map((status) => ({ value: status, label: REQUEST_STATUS_LABELS[status] }));

export const BulkRequestActions = ({ ids, getLabel, onDone }: IBulkRequestActionsProps) => {
  const [action, setAction] = useState<RequestBulkAction>(REQUEST_BULK_ACTIONS.archive);
  const [status, setStatus] = useState<RequestStatus>(REQUEST_STATUSES.confirmed);
  const [tag, setTag] = useState("");
  const [result, setResult] = useState<IBulkResult | null>(null);
  const { mutate: apply, isPending, error } = useBulkRequests();
  const needsTag = TAG_BULK_ACTIONS.includes(action);

  const submit = () =>
    apply(
      {
        ids,
        action,
        status: action === REQUEST_BULK_ACTIONS.changeStatus ? status : undefined,
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
        aria-label={BULK_REQUESTS_TEXT.action}
        options={BULK_ACTION_OPTIONS}
        value={action}
        onChange={(event) => setAction(event.target.value as RequestBulkAction)}
        className={TOOLBAR_CONTROL_CLASS_NAME}
      />
      {action === REQUEST_BULK_ACTIONS.changeStatus && (
        <SelectControl
          aria-label={BULK_REQUESTS_TEXT.status}
          options={STATUS_OPTIONS}
          value={status}
          onChange={(event) => setStatus(event.target.value as RequestStatus)}
          className={TOOLBAR_CONTROL_CLASS_NAME}
        />
      )}
      {needsTag && (
        <input
          aria-label={BULK_REQUESTS_TEXT.tag}
          placeholder={BULK_REQUESTS_TEXT.tag}
          value={tag}
          onChange={(event) => setTag(event.target.value)}
          className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-32 px-3")}
        />
      )}
      <Button className="h-8 px-3" isLoading={isPending} disabled={needsTag && !tag.trim()} onClick={submit}>
        {BULK_REQUESTS_TEXT.apply}
      </Button>
      <FormError message={error?.message} />
      <Modal isOpen={result !== null} title={BULK_REQUESTS_TEXT.resultTitle} onClose={closeResult}>
        {result && <BulkResult result={result} getLabel={getLabel} />}
        <ModalFooter>
          <Button onClick={closeResult}>{BULK_REQUESTS_TEXT.done}</Button>
        </ModalFooter>
      </Modal>
    </>
  );
};
