import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import { API_BASE_PATH, API_REQUEST_TIMEOUT_MS, HTTP_STATUS } from "@/shared/api/api.constant";
import { toApiError } from "@/shared/api/api-error";

export interface IApiClientConfig {
  refreshPath: string;
  loginRoute: string;
  ownAuthErrorPaths: readonly string[];
}

interface IRetriableRequest extends InternalAxiosRequestConfig {
  isRetry?: boolean;
}

export const createApiClient = ({ refreshPath, loginRoute, ownAuthErrorPaths }: IApiClientConfig): AxiosInstance => {
  const client = axios.create({
    baseURL: API_BASE_PATH,
    withCredentials: true,
    timeout: API_REQUEST_TIMEOUT_MS,
    paramsSerializer: { indexes: null },
  });
  let pendingRefresh: Promise<void> | null = null;

  const refreshSession = () => {
    pendingRefresh ??= client
      .post(refreshPath)
      .then(() => undefined)
      .finally(() => {
        pendingRefresh = null;
      });

    return pendingRefresh;
  };

  const canRefresh = (error: AxiosError, request?: IRetriableRequest): request is IRetriableRequest =>
    error.response?.status === HTTP_STATUS.unauthorized &&
    request !== undefined &&
    !request.isRetry &&
    request.url !== refreshPath &&
    !ownAuthErrorPaths.includes(request.url ?? "");

  client.interceptors.response.use(undefined, async (error: AxiosError) => {
    const request = error.config as IRetriableRequest | undefined;
    if (!canRefresh(error, request)) throw toApiError(error);

    try {
      await refreshSession();
    } catch (refreshError) {
      window.location.assign(loginRoute);
      throw toApiError(refreshError);
    }

    request.isRetry = true;
    return client(request);
  });

  return client;
};
