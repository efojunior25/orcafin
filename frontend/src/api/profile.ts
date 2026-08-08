import api from './client';

export interface Profile {
  phoneLinked: boolean;
  phoneNumber: string | null;
  defaultAccountId: string | null;
}

export interface PhoneCode {
  code: string;
  expiresInMinutes: number;
}

export async function getProfile(): Promise<Profile> {
  const { data } = await api.get<Profile>('/api/profile');
  return data;
}

export async function requestPhoneCode(): Promise<PhoneCode> {
  const { data } = await api.post<PhoneCode>('/api/profile/phone/request-code');
  return data;
}

export async function setDefaultAccount(accountId: string): Promise<Profile> {
  const { data } = await api.put<Profile>('/api/profile/default-account', { accountId });
  return data;
}
