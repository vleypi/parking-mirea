"use client";

import { useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { ownerService } from "@/entities/owner/services/owner.service";

export const useOwner = (id: string) => useQuery({ queryKey: QUERY_KEYS.owner(id), queryFn: () => ownerService.getById(id) });

export const useOwnerRelated = (id: string) =>
  useQuery({ queryKey: QUERY_KEYS.ownerRelated(id), queryFn: () => ownerService.getRelated(id) });
