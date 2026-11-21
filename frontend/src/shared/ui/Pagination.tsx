"use client";

import { Icon } from "@/shared/ui/Icon";
import { joinClassNames } from "@/shared/lib";
import { useEffect, useEffectEvent } from "react";
import { LIST_TEXT } from "@/shared/config";
import { countPages, getPageItems, PAGE_GAP } from "@/shared/lib";

interface IPaginationProps {
  page: number;
  pageSize: number;
  total: number;
  onChange: (page: number) => void;
}

const PAGE_BUTTON_CLASS_NAME = "grid h-8 min-w-8 place-items-center rounded-lg border px-2 text-sm tabular-nums transition disabled:pointer-events-none disabled:opacity-40";
const IDLE_PAGE_CLASS_NAME = "border-gray-200 bg-white text-gray-700 hover:bg-gray-50";
const CURRENT_PAGE_CLASS_NAME = "border-brand bg-brand text-white";

export const Pagination = ({ page, pageSize, total, onChange }: IPaginationProps) => {
  const pageCount = countPages(total, pageSize);
  const isOutOfRange = total > 0 && page > pageCount;

  const showLastPage = useEffectEvent(() => onChange(pageCount));

  useEffect(() => {
    if (isOutOfRange) showLastPage();
  }, [isOutOfRange]);

  if (pageCount <= 1 || isOutOfRange) return null;

  return (
    <nav aria-label={LIST_TEXT.pagination} className="mt-4 flex items-center justify-end gap-1 text-sm">
      <button type="button" onClick={() => onChange(page - 1)} disabled={page === 1} aria-label={LIST_TEXT.previousPage} className={joinClassNames(PAGE_BUTTON_CLASS_NAME, IDLE_PAGE_CLASS_NAME)}>
        <Icon name="chevronLeft" className="size-4" />
      </button>
      {getPageItems(page, pageCount).map((item, index) =>
        item === PAGE_GAP ? (
          <span key={`${PAGE_GAP}-${index}`} className="grid h-8 min-w-6 place-items-center text-gray-400">
            …
          </span>
        ) : (
          <button
            key={item}
            type="button"
            onClick={() => onChange(item)}
            aria-current={item === page ? "page" : undefined}
            className={joinClassNames(PAGE_BUTTON_CLASS_NAME, item === page ? CURRENT_PAGE_CLASS_NAME : IDLE_PAGE_CLASS_NAME)}
          >
            {item}
          </button>
        ),
      )}
      <button type="button" onClick={() => onChange(page + 1)} disabled={page === pageCount} aria-label={LIST_TEXT.nextPage} className={joinClassNames(PAGE_BUTTON_CLASS_NAME, IDLE_PAGE_CLASS_NAME)}>
        <Icon name="chevronRight" className="size-4" />
      </button>
    </nav>
  );
};
