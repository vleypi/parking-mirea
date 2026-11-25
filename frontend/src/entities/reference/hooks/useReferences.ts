"use client";

import { useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { referenceService } from "@/entities/reference/services/reference.service";

const REFERENCES_STALE_MS = 5 * 60 * 1000;

/** Все справочники одним запросом GET /api/references; кешируются на 5 минут. */
export const useReferences = () =>
  useQuery({ queryKey: QUERY_KEYS.references, queryFn: () => referenceService.getAll(), staleTime: REFERENCES_STALE_MS });
