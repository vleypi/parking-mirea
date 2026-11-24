import { apiClient, type IBulkResult, type IPage } from "@/shared/api";
import type {
  ISpot,
  ISpotBulkPayload,
  ISpotListRequest,
  ISpotPayload,
  ISpotRequests,
  ISpotUpdatePayload,
} from "@/entities/spot/interfaces/spot.interface";

export const SPOTS_PATH = "/spots";

class SpotService {
  async getPage(request: ISpotListRequest): Promise<IPage<ISpot>> {
    const { data } = await apiClient.get<IPage<ISpot>>(SPOTS_PATH, { params: request });
    return data;
  }

  async getById(id: string): Promise<ISpot> {
    const { data } = await apiClient.get<ISpot>(`${SPOTS_PATH}/${id}`);
    return data;
  }

  async getRequests(id: string): Promise<ISpotRequests> {
    const { data } = await apiClient.get<ISpotRequests>(`${SPOTS_PATH}/${id}/requests`);
    return data;
  }

  /** Работающие места без пересекающихся неотменённых заявок (from и to — локальное время ISO). */
  async getFree(from: string, to: string): Promise<ISpot[]> {
    const { data } = await apiClient.get<ISpot[]>(`${SPOTS_PATH}/free`, { params: { from, to } });
    return data;
  }

  async create(payload: ISpotPayload): Promise<ISpot> {
    const { data } = await apiClient.post<ISpot>(SPOTS_PATH, payload);
    return data;
  }

  async update(id: string, payload: ISpotUpdatePayload): Promise<ISpot> {
    const { data } = await apiClient.put<ISpot>(`${SPOTS_PATH}/${id}`, payload);
    return data;
  }

  async archive(id: string): Promise<ISpot> {
    const { data } = await apiClient.delete<ISpot>(`${SPOTS_PATH}/${id}`);
    return data;
  }

  async restore(id: string): Promise<ISpot> {
    const { data } = await apiClient.post<ISpot>(`${SPOTS_PATH}/${id}/restore`);
    return data;
  }

  async deletePermanently(id: string): Promise<void> {
    await apiClient.delete(`${SPOTS_PATH}/${id}/permanent`);
  }

  async bulk(payload: ISpotBulkPayload): Promise<IBulkResult> {
    const { data } = await apiClient.post<IBulkResult>(`${SPOTS_PATH}/bulk`, payload);
    return data;
  }
}

export const spotService = new SpotService();
