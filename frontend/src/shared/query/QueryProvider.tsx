"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { type ReactNode, useState } from "react";
import { createQueryClient } from "@/shared/query/create-query-client";

interface IQueryProviderProps {
  children: ReactNode;
}

export const QueryProvider = ({ children }: IQueryProviderProps) => {
  const [queryClient] = useState(createQueryClient);

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
};
