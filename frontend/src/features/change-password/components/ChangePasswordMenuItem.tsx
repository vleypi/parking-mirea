"use client";

import { useState } from "react";
import { Button, Icon, Modal } from "@/shared/ui";
import { ChangePasswordForm } from "@/features/change-password/components/ChangePasswordForm";
import { CHANGE_PASSWORD_MODE_TEXT, CHANGE_PASSWORD_MODES, CHANGE_PASSWORD_TEXT } from "@/features/change-password/constants/change-password.constant";

const TEXT = CHANGE_PASSWORD_MODE_TEXT[CHANGE_PASSWORD_MODES.profile];

export const ChangePasswordMenuItem = () => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <Button variant="ghost" onClick={() => setIsOpen(true)} className="w-full justify-start px-3">
        <Icon name="lock" className="size-4" />
        {CHANGE_PASSWORD_TEXT.menuItem}
      </Button>
      <Modal isOpen={isOpen} title={TEXT.title} onClose={() => setIsOpen(false)}>
        <div className="pb-6">
          <p className="mb-6 text-sm text-gray-500">{TEXT.description}</p>
          <ChangePasswordForm mode={CHANGE_PASSWORD_MODES.profile} onDone={() => setIsOpen(false)} />
        </div>
      </Modal>
    </>
  );
};
