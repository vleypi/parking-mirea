import { formatTags, parseTags } from "@/shared/lib";
import { CLIENT_CATEGORY_DEFAULT_ID, type IOwner, type IOwnerPayload, OWNER_STATUSES } from "@/entities/owner";
import type { OwnerFormValues } from "@/features/save-owner/schemas/owner-form.schema";

export const toOwnerFormValues = (owner: IOwner | null): OwnerFormValues =>
  owner
    ? {
        name: owner.name,
        phone: owner.phone,
        email: owner.email ?? "",
        categoryId: String(owner.category.id),
        status: owner.status.code,
        discountPercent: String(owner.discountPercent),
        notificationsEnabled: owner.notificationsEnabled,
        tags: formatTags(owner.tags),
      }
    : {
        name: "",
        phone: "",
        email: "",
        categoryId: String(CLIENT_CATEGORY_DEFAULT_ID),
        status: OWNER_STATUSES.active,
        discountPercent: "0",
        notificationsEnabled: true,
        tags: "",
      };

export const toOwnerPayload = (values: OwnerFormValues): IOwnerPayload => ({
  name: values.name.trim(),
  phone: values.phone.trim(),
  email: values.email.trim() || null,
  categoryId: Number(values.categoryId),
  status: values.status,
  discountPercent: Number(values.discountPercent.replace(",", ".")),
  notificationsEnabled: values.notificationsEnabled,
  tags: parseTags(values.tags),
});
