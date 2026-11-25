"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type IVehicle, vehicleService } from "@/entities/vehicle";
import type { VehicleFormValues } from "@/features/save-vehicle/schemas/vehicle-form.schema";
import { toVehiclePayload } from "@/features/save-vehicle/utils/vehicle-form.util";

/** Создание или изменение. При изменении уходит version: если запись уже поменяли, сервер ответит 409. */
export const useSaveVehicle = (vehicle: IVehicle | null) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (values: VehicleFormValues) =>
      vehicle
        ? vehicleService.update(vehicle.id, { ...toVehiclePayload(values), version: vehicle.version })
        : vehicleService.create(toVehiclePayload(values)),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.vehicles });
      void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.owners });
    },
  });
};
