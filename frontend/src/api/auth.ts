import api from './client';

export interface AuthResponse {
  token: string;
  name: string;
  email: string;
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/api/auth/login', { email, password });
  return data;
}

export async function register(name: string, email: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/api/auth/register', { name, email, password });
  return data;
}

export interface LoginAudit {
  id: string;
  ip: string;
  success: boolean;
  userAgent: string | null;
  createdAt: string;
}

export async function getLoginHistory(): Promise<LoginAudit[]> {
  const { data } = await api.get<LoginAudit[]>('/api/auth/login-history');
  return data;
}

export async function forgotPassword(email: string): Promise<{ message: string }> {
  const { data } = await api.post<{ message: string }>('/api/auth/forgot-password', { email });
  return data;
}

export async function resetPassword(token: string, newPassword: string): Promise<{ message: string }> {
  const { data } = await api.post<{ message: string }>('/api/auth/reset-password', { token, newPassword });
  return data;
}
