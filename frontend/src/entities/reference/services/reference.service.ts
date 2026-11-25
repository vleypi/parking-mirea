import { apiClient } from "@/shared/api";
import type { IReferences, ISpotType, ISpotTypePayload } from "@/entities/reference/interfaces/reference.interface";

const REFERENCES_PATH = "/references";
const SPOT_TYPES_PATH = `${REFERENCES_PATH}/spot-types`;

class ReferenceService {
  async getAll(): Promise<IReferences> {
    const { data } = await apiClient.get<IReferences>(REFERENCES_PATH);
    return data;
  }

  async createSpotType(payload: ISpotTypePayload): Promise<ISpotType> {
    const { data } = await apiClient.post<ISpotType>(SPOT_TYPES_PATH, payload);
    return data;
  }

  async updateSpotType(id: number, payload: ISpotTypePayload): Promise<ISpotType> {
    const { data } = await apiClient.put<ISpotType>(`${SPOT_TYPES_PATH}/${id}`, payload);
    return data;
  }

  async setSpotTypeActive(id: number, active: boolean): Promise<ISpotType> {
    const { data } = await apiClient.patch<ISpotType>(`${SPOT_TYPES_PATH}/${id}/active`, { active });
    return data;
  }
}

export const referenceService = new ReferenceService();
