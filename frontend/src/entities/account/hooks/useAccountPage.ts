"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import type { IAccountListRequest } from "@/entities/account/interfaces/account.interface";
import { accountService } from "@/entities/account/services/account.service";

export const useAccountPage = (request: IAccountListRequest) =>
  useQuery({ queryKey: QUERY_KEYS.accountPage(request), queryFn: () => accountService.getPage(request), placeholderData: keepPreviousData });
