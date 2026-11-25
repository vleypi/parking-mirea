"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type IVehicle, vehicleService } from "@/entities/vehicle";

export const useArchiveVehicle = (vehicle: IVehicle) => {
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.vehicles });

  return {
    toggleArchive: useMutation({
      mutationFn: () => (vehicle.archived ? vehicleService.restore(vehicle.id) : vehicleService.archive(vehicle.id)),
      onSuccess,
    }),
    removePermanently: useMutation({ mutationFn: () => vehicleService.deletePermanently(vehicle.id), onSuccess }),
  };
};
