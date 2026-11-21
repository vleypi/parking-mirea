import { CHANGE_PASSWORD_MODE_TEXT, CHANGE_PASSWORD_MODES } from "@/features/change-password/constants/change-password.constant";
import { ChangePasswordForm } from "@/features/change-password/components/ChangePasswordForm";

const TEXT = CHANGE_PASSWORD_MODE_TEXT[CHANGE_PASSWORD_MODES.temporary];

export const TemporaryPasswordScreen = () => (
  <div className="grid min-h-full place-items-center">
    <div className="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
      <h1 className="text-xl font-bold text-ink">{TEXT.title}</h1>
      <p className="mt-1 mb-6 text-sm text-gray-500">{TEXT.description}</p>
      <ChangePasswordForm mode={CHANGE_PASSWORD_MODES.temporary} />
    </div>
  </div>
);
