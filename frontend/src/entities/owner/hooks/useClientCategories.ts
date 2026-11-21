"use client";

import { useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { ownerService } from "@/entities/owner/services/owner.service";

const CATEGORIES_STALE_MS = 5 * 60 * 1000;

export const useClientCategories = () =>
  useQuery({ queryKey: QUERY_KEYS.clientCategories, queryFn: () => ownerService.getCategories(), staleTime: CATEGORIES_STALE_MS });
