"use client";

import { useState } from "react";
import { Button, FormError, Modal, TextField } from "@/shared/ui";
import { type IVehicle, useParkedVehicles, VehicleTable } from "@/entities/vehicle";
import { PARKED_VEHICLES_TEXT } from "@/features/parked-vehicles/constants/parked-vehicles.constant";

interface IParkedVehiclesButtonProps {
  onSelect: (vehicle: IVehicle) => void;
}

/** Кнопка «Стояли на парковке в период» (findParkedDuring из КР2) и окно с результатом. */
export const ParkedVehiclesButton = ({ onSelect }: IParkedVehiclesButtonProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const { data, isFetching, error } = useParkedVehicles(from, to);

  const select = (vehicle: IVehicle) => {
    setIsOpen(false);
    onSelect(vehicle);
  };

  return (
    <>
      <Button variant="secondary" className="w-full rounded-full" onClick={() => setIsOpen(true)}>
        {PARKED_VEHICLES_TEXT.open}
      </Button>
      <Modal size="xl" isOpen={isOpen} title={PARKED_VEHICLES_TEXT.title} onClose={() => setIsOpen(false)}>
        <div className="flex flex-col gap-4 pb-6">
          <p className="text-sm text-gray-500">{PARKED_VEHICLES_TEXT.description}</p>
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label={PARKED_VEHICLES_TEXT.from} type="datetime-local" value={from} onChange={(event) => setFrom(event.target.value)} />
            <TextField label={PARKED_VEHICLES_TEXT.to} type="datetime-local" value={to} onChange={(event) => setTo(event.target.value)} />
          </div>
          <FormError message={error?.message} />
          {from && to ? (
            <VehicleTable vehicles={data} isLoading={isFetching && !data} search="" emptyText={PARKED_VEHICLES_TEXT.empty} onSelect={select} />
          ) : (
            <p className="text-sm text-gray-500">{PARKED_VEHICLES_TEXT.pickPeriod}</p>
          )}
        </div>
      </Modal>
    </>
  );
};
