import api from './client';

export type VaultMovementType = 'APORTE' | 'RESGATE' | 'RENDIMENTO';

export interface Vault {
  id: string;
  name: string;
  balance: number;
  annualRate: number;
}

export interface VaultInput {
  name: string;
  annualRate: number;
}

export interface VaultMovement {
  id: string;
  type: VaultMovementType;
  amount: number;
  date: string;
}

export async function getVaults(): Promise<Vault[]> {
  const { data } = await api.get<Vault[]>('/api/vaults');
  return data;
}

export async function createVault(input: VaultInput): Promise<Vault> {
  const { data } = await api.post<Vault>('/api/vaults', input);
  return data;
}

export async function updateVault(id: string, input: VaultInput): Promise<Vault> {
  const { data } = await api.put<Vault>(`/api/vaults/${id}`, input);
  return data;
}

export async function deleteVault(id: string): Promise<void> {
  await api.delete(`/api/vaults/${id}`);
}

export async function getVaultMovements(id: string): Promise<VaultMovement[]> {
  const { data } = await api.get<VaultMovement[]>(`/api/vaults/${id}/movements`);
  return data;
}

export async function depositVault(id: string, amount: number): Promise<Vault> {
  const { data } = await api.post<Vault>(`/api/vaults/${id}/deposit`, { amount });
  return data;
}

export async function withdrawVault(id: string, amount: number): Promise<Vault> {
  const { data } = await api.post<Vault>(`/api/vaults/${id}/withdraw`, { amount });
  return data;
}
