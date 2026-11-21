import { apiClient, type IPage } from "@/shared/api";
import type { IRequest, IRequestListRequest } from "@/entities/request/interfaces/request.interface";

const REQUESTS_PATH = "/requests";

class RequestService {
  async getPage(request: IRequestListRequest): Promise<IPage<IRequest>> {
    const { data } = await apiClient.get<IPage<IRequest>>(REQUESTS_PATH, { params: request });
    return data;
  }
}

export const requestService = new RequestService();
