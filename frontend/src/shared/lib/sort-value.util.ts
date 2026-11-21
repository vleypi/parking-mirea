import { SORT_VALUE_SEPARATOR, type SortOrder } from "@/shared/config";

export const toSortValue = (field: string, order: SortOrder): string => [field, order].join(SORT_VALUE_SEPARATOR);
