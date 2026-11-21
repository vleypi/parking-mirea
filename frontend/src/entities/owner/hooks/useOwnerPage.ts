"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import type { IOwnerListRequest } from "@/entities/owner/interfaces/owner.interface";
import { ownerService } from "@/entities/owner/services/owner.service";

export const useOwnerPage = (request: IOwnerListRequest) =>
  useQuery({ queryKey: QUERY_KEYS.ownerPage(request), queryFn: () => ownerService.getPage(request), placeholderData: keepPreviousData });
