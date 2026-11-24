"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { type IVehicleBulkPayload, vehicleService } from "@/entities/vehicle";

export const useBulkVehicles = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: IVehicleBulkPayload) => vehicleService.bulk(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.vehicles }),
  });
};
