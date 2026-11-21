"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import type { IRequestListRequest } from "@/entities/request/interfaces/request.interface";
import { requestService } from "@/entities/request/services/request.service";

export const useRequestPage = (request: IRequestListRequest) =>
  useQuery({ queryKey: QUERY_KEYS.requestPage(request), queryFn: () => requestService.getPage(request), placeholderData: keepPreviousData });
