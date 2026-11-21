"use client";

import { useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { requestService } from "@/entities/request/services/request.service";

export const useRequest = (id: string) => useQuery({ queryKey: QUERY_KEYS.request(id), queryFn: () => requestService.getById(id) });
