"use client";

import { Button } from "@/shared/ui/Button";
import { FormError } from "@/shared/ui/FormError";
import { Modal } from "@/shared/ui/Modal";
import { ModalFooter } from "@/shared/ui/ModalFooter";
import { UI_TEXT } from "@/shared/ui/ui.constant";

interface IConfirmModalProps {
  isOpen: boolean;
  title: string;
  description: string;
  confirmText: string;
  isPending: boolean;
  error: string | undefined;
  onConfirm: () => void;
  onClose: () => void;
}

export const ConfirmModal = ({ isOpen, title, description, confirmText, isPending, error, onConfirm, onClose }: IConfirmModalProps) => (
  <Modal isOpen={isOpen} title={title} onClose={onClose}>
    <div className="flex flex-col gap-4">
      <p className="text-sm text-gray-600">{description}</p>
      <FormError message={error} />
    </div>
    <ModalFooter>
      <Button variant="secondary" onClick={onClose} disabled={isPending}>
        {UI_TEXT.cancel}
      </Button>
      <Button variant="danger" isLoading={isPending} onClick={onConfirm}>
        {confirmText}
      </Button>
    </ModalFooter>
  </Modal>
);
