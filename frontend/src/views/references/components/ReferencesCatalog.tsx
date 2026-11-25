"use client";

import { useState } from "react";
import { ROLES } from "@/shared/config";
import { useEntityModal } from "@/shared/lib";
import { Badge, CARD_CLASS_NAME, DataTable, FormError, type IDataTableColumn, Modal, PageHeader, SectionHeader } from "@/shared/ui";
import { type ICodeTitle, type ISpotType, useReferences } from "@/entities/reference";
import { hasRole, useViewer } from "@/entities/viewer";
import { SpotTypeForm, ToggleSpotTypeButton } from "@/features/manage-spot-types";
import { REFERENCES_PAGE_TEXT } from "@/views/references/constants/references-page.constant";

interface ICodeTitleListProps {
  title: string;
  items: readonly ICodeTitle[];
}

const CodeTitleList = ({ title, items }: ICodeTitleListProps) => (
  <div className={CARD_CLASS_NAME + " p-5"}>
    <h3 className="mb-4 text-xs font-semibold uppercase tracking-widest text-gray-400">{title}</h3>
    <ul className="flex flex-col gap-2 text-sm">
      {items.map(({ code, title: itemTitle }) => (
        <li key={code} className="flex items-center justify-between gap-3">
          <span className="text-ink">{itemTitle}</span>
          <span className="rounded-md bg-gray-100 px-2 py-0.5 font-mono text-xs text-gray-500">{code}</span>
        </li>
      ))}
    </ul>
  </div>
);

const getColumns = (canEdit: boolean, onError: (message: string) => void): IDataTableColumn<ISpotType>[] => [
  { key: "title", header: REFERENCES_PAGE_TEXT.name, render: ({ title }) => <span className="font-medium text-ink">{title}</span> },
  { key: "code", header: REFERENCES_PAGE_TEXT.code, render: ({ code }) => <span className="font-mono text-xs">{code}</span> },
  { key: "sortOrder", header: REFERENCES_PAGE_TEXT.sortOrder, render: ({ sortOrder }) => <span className="tabular-nums">{sortOrder}</span> },
  {
    key: "state",
    header: REFERENCES_PAGE_TEXT.state,
    render: ({ active }) => (
      <Badge tone={active ? "success" : "neutral"}>{active ? REFERENCES_PAGE_TEXT.active : REFERENCES_PAGE_TEXT.inactive}</Badge>
    ),
  },
  ...(canEdit
    ? [
        {
          key: "actions",
          header: REFERENCES_PAGE_TEXT.actions,
          render: (spotType: ISpotType) => <ToggleSpotTypeButton spotType={spotType} onError={onError} />,
        },
      ]
    : []),
];

/** Справочники: типы мест с изменением для ADMIN, остальные — только чтение. */
export const ReferencesCatalog = () => {
  const { data, isPending, isError } = useReferences();
  const { data: viewer } = useViewer();
  const modal = useEntityModal<ISpotType>();
  const [toggleError, setToggleError] = useState("");
  const canEdit = hasRole(viewer, ROLES.admin);

  return (
    <>
      <PageHeader
        title={REFERENCES_PAGE_TEXT.title}
        description={REFERENCES_PAGE_TEXT.description}
        add={canEdit ? { label: REFERENCES_PAGE_TEXT.add, onClick: modal.openCreate } : undefined}
      />
      {isError && <FormError message={REFERENCES_PAGE_TEXT.loadFailed} />}
      <section className="mb-10">
        <SectionHeader title={REFERENCES_PAGE_TEXT.spotTypesTitle} description={REFERENCES_PAGE_TEXT.spotTypesDescription} />
        <FormError message={toggleError} />
        <DataTable
          columns={getColumns(canEdit, setToggleError)}
          rows={data?.spotTypes}
          getRowKey={({ id }) => String(id)}
          isLoading={isPending}
          emptyText={REFERENCES_PAGE_TEXT.empty}
          onRowClick={canEdit ? modal.openEdit : undefined}
        />
      </section>
      {data && (
        <section>
          <SectionHeader title={REFERENCES_PAGE_TEXT.readOnlyTitle} description={REFERENCES_PAGE_TEXT.readOnlyDescription} />
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <CodeTitleList title={REFERENCES_PAGE_TEXT.requestStatuses} items={data.requestStatuses} />
            <CodeTitleList title={REFERENCES_PAGE_TEXT.clientCategories} items={data.clientCategories} />
            <CodeTitleList title={REFERENCES_PAGE_TEXT.ownerStatuses} items={data.ownerStatuses} />
            <CodeTitleList title={REFERENCES_PAGE_TEXT.vehicleStatuses} items={data.vehicleStatuses} />
            <CodeTitleList title={REFERENCES_PAGE_TEXT.spotStatuses} items={data.spotStatuses} />
          </div>
        </section>
      )}
      <Modal
        isOpen={modal.isOpen}
        title={modal.entity ? `${REFERENCES_PAGE_TEXT.editTitle} «${modal.entity.title}»` : REFERENCES_PAGE_TEXT.createTitle}
        onClose={modal.close}
      >
        <SpotTypeForm spotType={modal.entity} onDone={modal.close} />
      </Modal>
    </>
  );
};
