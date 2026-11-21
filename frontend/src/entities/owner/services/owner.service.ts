import { apiClient, type IBulkResult, type IPage } from "@/shared/api";
import type {
  IClientCategory,
  IOwner,
  IOwnerBulkPayload,
  IOwnerListRequest,
  IOwnerPayload,
  IOwnerUpdatePayload,
} from "@/entities/owner/interfaces/owner.interface";

const OWNERS_PATH = "/owners";
const CLIENT_CATEGORIES_PATH = "/references/client-categories";

class OwnerService {
  async getPage(request: IOwnerListRequest): Promise<IPage<IOwner>> {
    const { data } = await apiClient.get<IPage<IOwner>>(OWNERS_PATH, { params: request });
    return data;
  }

  async getById(id: string): Promise<IOwner> {
    const { data } = await apiClient.get<IOwner>(`${OWNERS_PATH}/${id}`);
    return data;
  }

  async create(payload: IOwnerPayload): Promise<IOwner> {
    const { data } = await apiClient.post<IOwner>(OWNERS_PATH, payload);
    return data;
  }

  async update(id: string, payload: IOwnerUpdatePayload): Promise<IOwner> {
    const { data } = await apiClient.put<IOwner>(`${OWNERS_PATH}/${id}`, payload);
    return data;
  }

  async archive(id: string): Promise<IOwner> {
    const { data } = await apiClient.delete<IOwner>(`${OWNERS_PATH}/${id}`);
    return data;
  }

  async restore(id: string): Promise<IOwner> {
    const { data } = await apiClient.post<IOwner>(`${OWNERS_PATH}/${id}/restore`);
    return data;
  }

  async deletePermanently(id: string): Promise<void> {
    await apiClient.delete(`${OWNERS_PATH}/${id}/permanent`);
  }

  async bulk(payload: IOwnerBulkPayload): Promise<IBulkResult> {
    const { data } = await apiClient.post<IBulkResult>(`${OWNERS_PATH}/bulk`, payload);
    return data;
  }

  async getCategories(): Promise<IClientCategory[]> {
    const { data } = await apiClient.get<IClientCategory[]>(CLIENT_CATEGORIES_PATH);
    return data;
  }
}

export const ownerService = new OwnerService();
