const TAG_SEPARATOR = ",";

export const parseTags = (text: string): string[] => [
  ...new Set(
    text
      .split(TAG_SEPARATOR)
      .map((tag) => tag.trim().toLowerCase())
      .filter(Boolean),
  ),
];

export const formatTags = (tags: readonly string[]): string => tags.join(`${TAG_SEPARATOR} `);
