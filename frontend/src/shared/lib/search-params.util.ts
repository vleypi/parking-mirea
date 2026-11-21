export type SearchParamChanges = Record<string, string | undefined>;

export const replaceSearchParams = (changes: SearchParamChanges): void => {
  const next = new URLSearchParams(window.location.search);
  for (const [key, value] of Object.entries(changes)) {
    if (value) next.set(key, value);
    else next.delete(key);
  }
  const query = next.toString();
  window.history.replaceState(null, "", query ? `${window.location.pathname}?${query}` : window.location.pathname);
};
