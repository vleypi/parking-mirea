"use client";

import { useState } from "react";
import { useEntityModal, useListParams } from "@/shared/lib";
import { Button, FormError, ListToolbar, Modal, ModalFooter, PageHeader, Pagination } from "@/shared/ui";
import {
  ACCOUNT_SORT_FIELDS,
  ACCOUNT_SORT_OPTIONS,
  ACCOUNT_TEXT,
  AccountTable,
  type IAccount,
  type ITemporaryPassword,
  TemporaryPasswordNotice,
  useAccountPage,
} from "@/entities/account";
import { ResetAccountPasswordButton } from "@/features/reset-account-password";
import { AccountForm } from "@/features/save-account";
import { USERS_PAGE_TEXT } from "@/views/users/constants/users-page.constant";

const ACCOUNT_SORT_FIELD_LIST = Object.values(ACCOUNT_SORT_FIELDS);

export const UsersCatalog = () => {
  const list = useListParams(ACCOUNT_SORT_FIELD_LIST);
  const { data, isPending, error } = useAccountPage(list.request);
  const modal = useEntityModal<IAccount>();
  const [issued, setIssued] = useState<ITemporaryPassword | null>(null);

  const showIssued = (result: ITemporaryPassword) => {
    modal.close();
    setIssued(result);
  };

  return (
    <>
      <PageHeader title={USERS_PAGE_TEXT.title} description={USERS_PAGE_TEXT.description} add={{ label: USERS_PAGE_TEXT.add, onClick: modal.openCreate }} />
      <ListToolbar list={list} placeholder={ACCOUNT_TEXT.searchPlaceholder} total={data?.totalElements} sortOptions={ACCOUNT_SORT_OPTIONS} />
      <FormError message={error?.message} />
      <AccountTable
        accounts={data?.content}
        isLoading={isPending}
        search={list.search}
        emptyText={list.getEmptyText(ACCOUNT_TEXT.empty)}
        sorting={list.sorting}
        onSelect={modal.openEdit}
      />
      <Pagination page={list.page} pageSize={list.pageSize} total={data?.totalElements ?? 0} onChange={list.setPage} />
      <Modal isOpen={modal.isOpen} title={modal.entity ? USERS_PAGE_TEXT.editTitle : USERS_PAGE_TEXT.createTitle} onClose={modal.close}>
        {modal.entity && (
          <div className="mb-4 flex justify-end">
            <ResetAccountPasswordButton account={modal.entity} onReset={showIssued} />
          </div>
        )}
        <AccountForm account={modal.entity} onCreated={showIssued} onDone={modal.close} />
      </Modal>
      <Modal isOpen={issued !== null} title={USERS_PAGE_TEXT.issuedTitle} onClose={() => setIssued(null)}>
        {issued && <TemporaryPasswordNotice login={issued.user.login} password={issued.temporaryPassword} />}
        <ModalFooter>
          <Button onClick={() => setIssued(null)}>{USERS_PAGE_TEXT.done}</Button>
        </ModalFooter>
      </Modal>
    </>
  );
};
