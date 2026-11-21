"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { EmptyState } from "@/shared/ui";
import { getAvailableNavItems, useViewer } from "@/entities/viewer";
import { HOME_TEXT } from "@/views/home/constants/home.constant";

export const HomePage = () => {
  const router = useRouter();
  const { data: viewer } = useViewer();
  const [firstSection] = getAvailableNavItems(viewer);

  useEffect(() => {
    if (firstSection) router.replace(firstSection.href);
  }, [firstSection, router]);

  if (!viewer || firstSection) return null;

  return <EmptyState icon="lock" title={HOME_TEXT.title} description={HOME_TEXT.description} />;
};
