import { apiClient, type IPage } from "@/shared/api";
import type {
  IAccount,
  IAccountListRequest,
  IAccountPayload,
  IAccountUpdatePayload,
  ITemporaryPassword,
} from "@/entities/account/interfaces/account.interface";

const ACCOUNTS_PATH = "/admin/users";

class AccountService {
  async getPage(request: IAccountListRequest): Promise<IPage<IAccount>> {
    const { data } = await apiClient.get<IPage<IAccount>>(ACCOUNTS_PATH, { params: request });
    return data;
  }

  async create(payload: IAccountPayload): Promise<ITemporaryPassword> {
    const { data } = await apiClient.post<ITemporaryPassword>(ACCOUNTS_PATH, payload);
    return data;
  }

  async update(id: string, payload: IAccountUpdatePayload): Promise<IAccount> {
    const { data } = await apiClient.put<IAccount>(`${ACCOUNTS_PATH}/${id}`, payload);
    return data;
  }

  async resetPassword(id: string): Promise<ITemporaryPassword> {
    const { data } = await apiClient.post<ITemporaryPassword>(`${ACCOUNTS_PATH}/${id}/reset-password`);
    return data;
  }
}

export const accountService = new AccountService();
