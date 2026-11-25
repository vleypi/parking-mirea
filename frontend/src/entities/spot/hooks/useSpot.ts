"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import type { ISpotListRequest } from "@/entities/spot/interfaces/spot.interface";
import { spotService } from "@/entities/spot/services/spot.service";

export const useSpotPage = (request: ISpotListRequest) =>
  useQuery({ queryKey: QUERY_KEYS.spotPage(request), queryFn: () => spotService.getPage(request), placeholderData: keepPreviousData });

export const useSpot = (id: string) => useQuery({ queryKey: QUERY_KEYS.spot(id), queryFn: () => spotService.getById(id) });

export const useSpotRequests = (id: string) => useQuery({ queryKey: QUERY_KEYS.spotRequests(id), queryFn: () => spotService.getRequests(id) });

/** Запрос уходит, только когда оба конца периода заданы. */
export const useFreeSpots = (from: string, to: string) =>
  useQuery({
    queryKey: QUERY_KEYS.freeSpots(from, to),
    queryFn: () => spotService.getFree(from, to),
    enabled: Boolean(from && to),
  });
