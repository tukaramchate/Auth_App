import useAuth from "@/auth/store";
import axios, {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import toast from "react-hot-toast";

type LoginResponseData = {
  accessToken: string;
  expiresIn: number;
  tokenType: string;
  user: {
    id: string;
    email: string;
    name?: string;
    enabled: boolean;
    provider: string;
    roles?: Array<{ name: string }>;
  };
};

type RetryRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

type PendingCallback = (newToken: string) => void;

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8083/api/v1";

const apiClient = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
  timeout: 10000,
});

export const refreshClient = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
  timeout: 10000,
});

apiClient.interceptors.request.use((config) => {
  const accessToken = useAuth.getState().accessToken;
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

let isRefreshing = false;
let pending: PendingCallback[] = [];

function queueRequest(cb: PendingCallback) {
  pending.push(cb);
}

function resolveQueue(newToken: string) {
  pending.forEach((cb) => cb(newToken));
  pending = [];
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const status = error?.response?.status;
    if (status === 403) {
      toast.error("Access denied");
      return Promise.reject(error);
    }

    const is401 = status === 401;
    const original = error.config as RetryRequestConfig | undefined;
    if (!original) {
      return Promise.reject(error);
    }

    const isRefreshEndpoint = original.url?.includes("/auth/refresh") ?? false;
    if (!is401 || original._retry || isRefreshEndpoint) {
      return Promise.reject(error);
    }

    original._retry = true;

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        queueRequest((newToken: string) => {
          if (!newToken) {
            reject(error);
            return;
          }

          original.headers.Authorization = `Bearer ${newToken}`;
          resolve(apiClient(original));
        });
      });
    }

    isRefreshing = true;

    try {
      const loginResponse: AxiosResponse<LoginResponseData> =
        await refreshClient.post<LoginResponseData>("/auth/refresh");
      const loginResponseData = loginResponse.data;
      const newToken = loginResponseData.accessToken;

      if (!newToken) {
        throw new Error("No access token received");
      }

      useAuth
        .getState()
        .changeLocalLoginData(newToken, loginResponseData.user, true);

      resolveQueue(newToken);
      original.headers.Authorization = `Bearer ${newToken}`;
      return apiClient(original);
    } catch (refreshError) {
      resolveQueue("");
      useAuth.getState().logout();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default apiClient;
