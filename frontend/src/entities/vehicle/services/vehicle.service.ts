import { apiClient, type IBulkResult, type IPage } from "@/shared/api";
import type {
  IVehicle,
  IVehicleBulkPayload,
  IVehicleListRequest,
  IVehiclePayload,
  IVehicleRequests,
  IVehicleUpdatePayload,
} from "@/entities/vehicle/interfaces/vehicle.interface";

export const VEHICLES_PATH = "/vehicles";

class VehicleService {
  async getPage(request: IVehicleListRequest): Promise<IPage<IVehicle>> {
    const { data } = await apiClient.get<IPage<IVehicle>>(VEHICLES_PATH, { params: request });
    return data;
  }

  async getById(id: string): Promise<IVehicle> {
    const { data } = await apiClient.get<IVehicle>(`${VEHICLES_PATH}/${id}`);
    return data;
  }

  async getRequests(id: string): Promise<IVehicleRequests> {
    const { data } = await apiClient.get<IVehicleRequests>(`${VEHICLES_PATH}/${id}/requests`);
    return data;
  }

  async create(payload: IVehiclePayload): Promise<IVehicle> {
    const { data } = await apiClient.post<IVehicle>(VEHICLES_PATH, payload);
    return data;
  }

  async update(id: string, payload: IVehicleUpdatePayload): Promise<IVehicle> {
    const { data } = await apiClient.put<IVehicle>(`${VEHICLES_PATH}/${id}`, payload);
    return data;
  }

  async archive(id: string): Promise<IVehicle> {
    const { data } = await apiClient.delete<IVehicle>(`${VEHICLES_PATH}/${id}`);
    return data;
  }

  async restore(id: string): Promise<IVehicle> {
    const { data } = await apiClient.post<IVehicle>(`${VEHICLES_PATH}/${id}/restore`);
    return data;
  }

  async deletePermanently(id: string): Promise<void> {
    await apiClient.delete(`${VEHICLES_PATH}/${id}/permanent`);
  }

  async bulk(payload: IVehicleBulkPayload): Promise<IBulkResult> {
    const { data } = await apiClient.post<IBulkResult>(`${VEHICLES_PATH}/bulk`, payload);
    return data;
  }
}

export const vehicleService = new VehicleService();
