"use client";

import { Button } from "@/shared/ui";
import type { ISpotType } from "@/entities/reference";
import { MANAGE_SPOT_TYPES_TEXT } from "@/features/manage-spot-types/constants/manage-spot-types.constant";
import { useToggleSpotType } from "@/features/manage-spot-types/hooks/useManageSpotTypes";

interface IToggleSpotTypeButtonProps {
  spotType: ISpotType;
  onError: (message: string) => void;
}

/** Тип не удаляется: отключённый пропадает из выбора в формах, но остаётся у старых мест. */
export const ToggleSpotTypeButton = ({ spotType, onError }: IToggleSpotTypeButtonProps) => {
  const { mutate: toggle, isPending } = useToggleSpotType();

  return (
    <Button
      variant="secondary"
      className="h-8 px-3"
      isLoading={isPending}
      onClick={(event) => {
        event.stopPropagation();
        toggle(spotType, { onError: (error) => onError(error.message) });
      }}
    >
      {spotType.active ? MANAGE_SPOT_TYPES_TEXT.disable : MANAGE_SPOT_TYPES_TEXT.enable}
    </Button>
  );
};
