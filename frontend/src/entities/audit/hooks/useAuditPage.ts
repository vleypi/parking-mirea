"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import type { IAuditListRequest } from "@/entities/audit/interfaces/audit.interface";
import { auditService } from "@/entities/audit/services/audit.service";

export const useAuditPage = (request: IAuditListRequest) =>
  useQuery({ queryKey: QUERY_KEYS.auditPage(request), queryFn: () => auditService.getPage(request), placeholderData: keepPreviousData });
