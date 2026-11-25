"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import type { IVehicleListRequest } from "@/entities/vehicle/interfaces/vehicle.interface";
import { vehicleService } from "@/entities/vehicle/services/vehicle.service";

export const useVehiclePage = (request: IVehicleListRequest, enabled = true) =>
  useQuery({
    queryKey: QUERY_KEYS.vehiclePage(request),
    queryFn: () => vehicleService.getPage(request),
    placeholderData: keepPreviousData,
    enabled,
  });

export const useVehicle = (id: string) => useQuery({ queryKey: QUERY_KEYS.vehicle(id), queryFn: () => vehicleService.getById(id) });

export const useVehicleRequests = (id: string) =>
  useQuery({ queryKey: QUERY_KEYS.vehicleRequests(id), queryFn: () => vehicleService.getRequests(id) });

/** Запрос уходит, только когда оба конца периода заданы. */
export const useParkedVehicles = (from: string, to: string) =>
  useQuery({
    queryKey: QUERY_KEYS.parkedVehicles(from, to),
    queryFn: () => vehicleService.getParked(from, to),
    enabled: Boolean(from && to),
  });
