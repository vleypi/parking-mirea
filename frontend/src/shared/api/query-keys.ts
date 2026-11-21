import type { IListRequest } from "@/shared/api/page.interface";

export const QUERY_KEYS = {
  viewer: ["viewer"],
  requests: ["requests"],
  requestPage: (request: IListRequest<string>) => ["requests", request],
} as const;
