"use client";

import { useEffect, useEffectEvent, useState } from "react";
import { SEARCH_DEBOUNCE_MS } from "@/shared/config";

export const useDebouncedDraft = (value: string, onCommit: (value: string) => void) => {
  const [draft, setDraft] = useState(value);
  const [syncedValue, setSyncedValue] = useState(value);
  const [sentValue, setSentValue] = useState(value);

  if (value !== syncedValue) {
    setSyncedValue(value);
    if (value !== sentValue) setDraft(value);
  }

  const commit = (nextValue: string) => {
    setSentValue(nextValue);
    onCommit(nextValue);
  };
  const commitAfterPause = useEffectEvent(commit);

  useEffect(() => {
    const nextValue = draft.trim();
    if (nextValue === value) return;

    const timer = setTimeout(() => commitAfterPause(nextValue), SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [draft, value]);

  const clear = () => {
    setDraft("");
    commit("");
  };

  return { draft, setDraft, clear };
};
