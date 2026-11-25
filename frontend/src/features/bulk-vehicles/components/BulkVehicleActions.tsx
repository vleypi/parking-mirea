"use client";

import { useState } from "react";
import type { IBulkResult } from "@/shared/api";
import { joinClassNames } from "@/shared/lib";
import { BulkResult, Button, FormError, Modal, ModalFooter, SelectControl, TOOLBAR_CONTROL_CLASS_NAME } from "@/shared/ui";
import { VEHICLE_BULK_ACTIONS, VEHICLE_STATUS_LABELS, VEHICLE_STATUSES, type VehicleBulkAction, type VehicleStatus } from "@/entities/vehicle";
import { BULK_ACTION_OPTIONS, BULK_VEHICLES_TEXT, TAG_BULK_ACTIONS } from "@/features/bulk-vehicles/constants/bulk-vehicles.constant";
import { useBulkVehicles } from "@/features/bulk-vehicles/hooks/useBulkVehicles";

interface IBulkVehicleActionsProps {
  ids: string[];
  getLabel: (id: string) => string;
  onDone: () => void;
}

const STATUS_OPTIONS = Object.values(VEHICLE_STATUSES).map((status) => ({ value: status, label: VEHICLE_STATUS_LABELS[status] }));

export const BulkVehicleActions = ({ ids, getLabel, onDone }: IBulkVehicleActionsProps) => {
  const [action, setAction] = useState<VehicleBulkAction>(VEHICLE_BULK_ACTIONS.archive);
  const [status, setStatus] = useState<VehicleStatus>(VEHICLE_STATUSES.blocked);
  const [tag, setTag] = useState("");
  const [result, setResult] = useState<IBulkResult | null>(null);
  const { mutate: apply, isPending, error } = useBulkVehicles();
  const needsTag = TAG_BULK_ACTIONS.includes(action);

  const submit = () =>
    apply(
      {
        ids,
        action,
        status: action === VEHICLE_BULK_ACTIONS.changeStatus ? status : undefined,
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
        aria-label={BULK_VEHICLES_TEXT.action}
        options={BULK_ACTION_OPTIONS}
        value={action}
        onChange={(event) => setAction(event.target.value as VehicleBulkAction)}
        className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-auto")}
      />
      {action === VEHICLE_BULK_ACTIONS.changeStatus && (
        <SelectControl
          aria-label={BULK_VEHICLES_TEXT.status}
          options={STATUS_OPTIONS}
          value={status}
          onChange={(event) => setStatus(event.target.value as VehicleStatus)}
          className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-auto")}
        />
      )}
      {needsTag && (
        <input
          aria-label={BULK_VEHICLES_TEXT.tag}
          placeholder={BULK_VEHICLES_TEXT.tag}
          value={tag}
          onChange={(event) => setTag(event.target.value)}
          className={joinClassNames(TOOLBAR_CONTROL_CLASS_NAME, "w-32 px-3")}
        />
      )}
      <Button className="h-8 px-3" isLoading={isPending} disabled={needsTag && !tag.trim()} onClick={submit}>
        {BULK_VEHICLES_TEXT.apply}
      </Button>
      <FormError message={error?.message} />
      <Modal isOpen={result !== null} title={BULK_VEHICLES_TEXT.resultTitle} onClose={closeResult}>
        {result && <BulkResult result={result} getLabel={getLabel} />}
        <ModalFooter>
          <Button onClick={closeResult}>{BULK_VEHICLES_TEXT.done}</Button>
        </ModalFooter>
      </Modal>
    </>
  );
};
