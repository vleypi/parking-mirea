"use client";

import { useState } from "react";

type ModalState<TEntity> = { isOpen: false } | { isOpen: true; entity: TEntity | null };

export const useEntityModal = <TEntity>() => {
  const [state, setState] = useState<ModalState<TEntity>>({ isOpen: false });

  return {
    isOpen: state.isOpen,
    entity: state.isOpen ? state.entity : null,
    openCreate: () => setState({ isOpen: true, entity: null }),
    openEdit: (entity: TEntity) => setState({ isOpen: true, entity }),
    close: () => setState({ isOpen: false }),
  };
};
