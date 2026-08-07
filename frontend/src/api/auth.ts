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
