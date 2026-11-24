"use client";

import { useState } from "react";
import { Button, FormError, Modal, TextField } from "@/shared/ui";
import { type ISpot, SpotTable, useFreeSpots } from "@/entities/spot";
import { FREE_SPOTS_TEXT } from "@/features/free-spots/constants/free-spots.constant";

interface IFreeSpotsButtonProps {
  onSelect: (spot: ISpot) => void;
}

/** Кнопка «Свободные места на период» (findFreeSpots из КР2) и окно с результатом. */
export const FreeSpotsButton = ({ onSelect }: IFreeSpotsButtonProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const { data, isFetching, error } = useFreeSpots(from, to);

  const select = (spot: ISpot) => {
    setIsOpen(false);
    onSelect(spot);
  };

  return (
    <>
      <Button variant="secondary" onClick={() => setIsOpen(true)}>
        {FREE_SPOTS_TEXT.open}
      </Button>
      <Modal size="xl" isOpen={isOpen} title={FREE_SPOTS_TEXT.title} onClose={() => setIsOpen(false)}>
        <div className="flex flex-col gap-4 pb-6">
          <p className="text-sm text-gray-500">{FREE_SPOTS_TEXT.description}</p>
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label={FREE_SPOTS_TEXT.from} type="datetime-local" value={from} onChange={(event) => setFrom(event.target.value)} />
            <TextField label={FREE_SPOTS_TEXT.to} type="datetime-local" value={to} onChange={(event) => setTo(event.target.value)} />
          </div>
          <FormError message={error?.message} />
          {from && to ? (
            <SpotTable spots={data} isLoading={isFetching && !data} search="" emptyText={FREE_SPOTS_TEXT.empty} onSelect={select} />
          ) : (
            <p className="text-sm text-gray-500">{FREE_SPOTS_TEXT.pickPeriod}</p>
          )}
        </div>
      </Modal>
    </>
  );
};
