import { FIRST_PAGE } from "@/shared/config";

export const PAGE_GAP = "gap";

const SIBLING_PAGES = 1;
const COMPACT_PAGE_LIMIT = 7;

export type PageItem = number | typeof PAGE_GAP;

export const countPages = (total: number, pageSize: number): number => Math.max(FIRST_PAGE, Math.ceil(total / pageSize));

export const getPageItems = (page: number, pageCount: number): PageItem[] => {
  if (pageCount <= COMPACT_PAGE_LIMIT) return Array.from({ length: pageCount }, (_, index) => index + FIRST_PAGE);

  const start = Math.max(FIRST_PAGE + 1, page - SIBLING_PAGES);
  const end = Math.min(pageCount - 1, page + SIBLING_PAGES);
  const middle = Array.from({ length: end - start + 1 }, (_, index) => start + index);

  const leadingGap: PageItem[] = start > FIRST_PAGE + 1 ? [PAGE_GAP] : [];
  const trailingGap: PageItem[] = end < pageCount - 1 ? [PAGE_GAP] : [];

  return [FIRST_PAGE, ...leadingGap, ...middle, ...trailingGap, pageCount];
};
