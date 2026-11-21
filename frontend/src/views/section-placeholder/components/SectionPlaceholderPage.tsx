import type { Role } from "@/shared/config";
import { EmptyState, PageHeader } from "@/shared/ui";
import { RequireRole } from "@/entities/viewer";
import { SECTION_PLACEHOLDER_TEXT } from "@/views/section-placeholder/constants/section-placeholder.constant";

interface ISectionPlaceholderPageProps {
  title: string;
  minRole: Role;
}

export const SectionPlaceholderPage = ({ title, minRole }: ISectionPlaceholderPageProps) => (
  <RequireRole minRole={minRole}>
    <PageHeader title={title} />
    <EmptyState icon="cog" title={SECTION_PLACEHOLDER_TEXT.title} description={SECTION_PLACEHOLDER_TEXT.description} />
  </RequireRole>
);
