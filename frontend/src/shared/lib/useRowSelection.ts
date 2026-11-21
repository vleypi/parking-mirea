"use client";

import { useState } from "react";
import type { IRowSelection } from "@/shared/lib/list.interface";

export const useRowSelection = (): IRowSelection => {
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());

  const toggle = (key: string) =>
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });

  const toggleAll = (keys: readonly string[]) =>
    setSelected((current) => {
      const next = new Set(current);
      const isEverySelected = keys.every((key) => next.has(key));
      keys.forEach((key) => (isEverySelected ? next.delete(key) : next.add(key)));
      return next;
    });

  return {
    ids: [...selected],
    isSelected: (key: string) => selected.has(key),
    toggle,
    toggleAll,
    clear: () => setSelected(new Set()),
  };
};
