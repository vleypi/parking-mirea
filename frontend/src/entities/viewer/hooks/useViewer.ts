"use client";

import { useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@/shared/api";
import { viewerService } from "@/entities/viewer/services/viewer.service";

export const useViewer = () => useQuery({ queryKey: QUERY_KEYS.viewer, queryFn: () => viewerService.getMe() });
