export const PREFERENCE_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365;

export const writeCookie = (name: string, value: string, maxAgeSeconds = PREFERENCE_COOKIE_MAX_AGE_SECONDS): void => {
  document.cookie = `${name}=${encodeURIComponent(value)}; path=/; max-age=${maxAgeSeconds}; samesite=lax`;
};
