import type { ReactNode } from "react";

interface IModalFooterProps {
  children: ReactNode;
}

export const ModalFooter = ({ children }: IModalFooterProps) => (
  <div className="sticky bottom-0 -mx-6 mt-2 flex flex-wrap items-center justify-end gap-2 border-t border-gray-100 bg-white px-6 py-4">{children}</div>
);
