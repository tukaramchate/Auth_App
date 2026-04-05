import type RegisterData from "@/models/RegisterData";
import apiClient, { refreshClient } from "@/config/ApiClient";
import type LoginData from "@/models/LoginData";
import type LoginResponseData from "@/models/LoginResponseData";
import type User from "@/models/User";
import type Session from "@/models/Session";
import type PageResponse from "@/models/PageResponse";
//register function
export const registerUser = async (signupData: RegisterData) => {
  // api  call to server to save data
  const response = await apiClient.post(`/auth/register`, signupData);
  return response.data;
};

//login

export const loginUser = async (loginData: LoginData) => {
  const response = await apiClient.post<LoginResponseData>(
    "/auth/login",
    loginData
  );
  return response.data;
};

export const logoutUser = async () => {
  const response = await apiClient.post(`/auth/logout`);
  return response.data;
};

//get current login user
export const getCurrentUser = async () => {
  const response = await apiClient.get<User>(`/users/me`);
  return response.data;
};

type CurrentUserProfileUpdatePayload = {
  name: string;
  image?: string;
};

export const updateCurrentUserProfile = async (payload: CurrentUserProfileUpdatePayload) => {
  const response = await apiClient.put<User>(`/users/me`, {
    name: payload.name,
    image: payload.image,
  });
  return response.data;
};

//refresh token

export const refreshToken = async () => {
  const response = await refreshClient.post<LoginResponseData>(`/auth/refresh`);
  return response.data;
};

export const verifyEmail = async (token: string) => {
  const response = await apiClient.get<{ message: string }>(
    `/auth/verify-email?token=${encodeURIComponent(token)}`
  );
  return response.data;
};

export const resendVerification = async (email: string) => {
  const response = await apiClient.post<{ message: string }>(
    `/auth/resend-verification`,
    { email }
  );
  return response.data;
};

export const forgotPassword = async (email: string) => {
  const response = await apiClient.post<{ message: string }>(
    `/auth/forgot-password`,
    { email }
  );
  return response.data;
};

export const resetPassword = async (token: string, newPassword: string) => {
  const response = await apiClient.post<{ message: string }>(
    `/auth/reset-password`,
    { token, newPassword }
  );
  return response.data;
};

type UserListQuery = {
  page?: number;
  size?: number;
  sort?: string;
  q?: string;
  enabled?: boolean;
  role?: string;
};

export const getAllUsersByAdmin = async (query: UserListQuery = {}) => {
  const response = await apiClient.get<PageResponse<User>>(`/users`, {
    params: {
      page: query.page ?? 0,
      size: query.size ?? 20,
      sort: query.sort ?? "createdAt,desc",
      q: query.q,
      enabled: query.enabled,
      role: query.role,
    },
  });
  return response.data;
};

type AdminCreateUserPayload = {
  email: string;
  name: string;
  password: string;
  enabled?: boolean;
  roles?: Array<{ name: string }>;
};

export const createUserByAdmin = async (payload: AdminCreateUserPayload) => {
  const response = await apiClient.post<User>(`/users`, {
    email: payload.email,
    name: payload.name,
    password: payload.password,
    enable: payload.enabled ?? true,
  });
  return response.data;
};

type AdminUpdateUserPayload = {
  name?: string;
  image?: string;
  enabled?: boolean;
  roles?: Array<{ name: string }>;
};

export const updateUserByAdmin = async (userId: string, payload: AdminUpdateUserPayload) => {
  const response = await apiClient.put<User>(`/users/${userId}/admin`, {
    name: payload.name,
    image: payload.image,
    enable: payload.enabled,
    roles: payload.roles,
  });
  return response.data;
};

export const getSystemRoles = async () => {
  const response = await apiClient.get<string[]>(`/users/roles`);
  return response.data;
};

export const getUserSessions = async () => {
  const response = await apiClient.get<Session[]>(`/users/me/sessions`);
  return response.data;
};

export const revokeCurrentUserSession = async (jti: string) => {
  await apiClient.delete(`/users/me/sessions/${encodeURIComponent(jti)}`);
};

export const revokeAllCurrentUserSessions = async () => {
  await apiClient.delete(`/users/me/sessions`);
};

export const getUserSessionsByAdmin = async (userId: string) => {
  const response = await apiClient.get<Session[]>(`/users/${userId}/sessions`);
  return response.data;
};

export const revokeUserSessionByAdmin = async (userId: string, jti: string) => {
  await apiClient.delete(`/users/${userId}/sessions/${encodeURIComponent(jti)}`);
};

export const revokeAllUserSessionsByAdmin = async (userId: string) => {
  await apiClient.delete(`/users/${userId}/sessions`);
};

export const deleteUserByAdmin = async (userId: string) => {
  await apiClient.delete(`/users/${userId}`);
};