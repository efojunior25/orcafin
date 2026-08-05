import api from './client';

export type AccountType = 'CORRENTE' | 'POUPANCA' | 'CARTEIRA';

export interface Account {
  id: number;
  name: string;
  type: AccountType;
  balance: number;
}

export interface AccountInput {
  name: string;
  type: AccountType;
}

export async function getAccounts(): Promise<Account[]> {
  const { data } = await api.get<Account[]>('/api/accounts');
  return data;
}

export async function createAccount(input: AccountInput): Promise<Account> {
  const { data } = await api.post<Account>('/api/accounts', input);
  return data;
}

export async function updateAccount(id: number, input: AccountInput): Promise<Account> {
  const { data } = await api.put<Account>(`/api/accounts/${id}`, input);
  return data;
}

export async function deleteAccount(id: number): Promise<void> {
  await api.delete(`/api/accounts/${id}`);
}
