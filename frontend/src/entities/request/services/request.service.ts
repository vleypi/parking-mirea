import { apiClient, type IBulkResult, type IPage } from "@/shared/api";
import type {
  IRequest,
  IRequestBulkPayload,
  IRequestListRequest,
  IRequestUpdatePayload,
} from "@/entities/request/interfaces/request.interface";

const REQUESTS_PATH = "/requests";

class RequestService {
  async getPage(request: IRequestListRequest): Promise<IPage<IRequest>> {
    const { data } = await apiClient.get<IPage<IRequest>>(REQUESTS_PATH, { params: request });
    return data;
  }

  async getById(id: string): Promise<IRequest> {
    const { data } = await apiClient.get<IRequest>(`${REQUESTS_PATH}/${id}`);
    return data;
  }

  async update(id: string, payload: IRequestUpdatePayload): Promise<IRequest> {
    const { data } = await apiClient.put<IRequest>(`${REQUESTS_PATH}/${id}`, payload);
    return data;
  }

  async archive(id: string): Promise<IRequest> {
    const { data } = await apiClient.delete<IRequest>(`${REQUESTS_PATH}/${id}`);
    return data;
  }

  async restore(id: string): Promise<IRequest> {
    const { data } = await apiClient.post<IRequest>(`${REQUESTS_PATH}/${id}/restore`);
    return data;
  }

  async deletePermanently(id: string): Promise<void> {
    await apiClient.delete(`${REQUESTS_PATH}/${id}/permanent`);
  }

  async bulk(payload: IRequestBulkPayload): Promise<IBulkResult> {
    const { data } = await apiClient.post<IBulkResult>(`${REQUESTS_PATH}/bulk`, payload);
    return data;
  }
}

export const requestService = new RequestService();
