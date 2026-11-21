import type { IListRequest } from "@/shared/api/page.interface";

export const QUERY_KEYS = {
  viewer: ["viewer"],
  requests: ["requests"],
  requestPage: (request: IListRequest<string>) => ["requests", "page", request],
  request: (id: string) => ["requests", "one", id],
  notes: (basePath: string) => ["notes", basePath],
  owners: ["owners"],
  ownerPage: (request: IListRequest<string>) => ["owners", "page", request],
  owner: (id: string) => ["owners", "one", id],
  ownerRelated: (id: string) => ["owners", "related", id],
  clientCategories: ["client-categories"],
  accounts: ["accounts"],
  accountPage: (request: IListRequest<string>) => ["accounts", request],
  auditPage: (request: IListRequest<string>) => ["audit", request],
} as const;
