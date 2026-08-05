import api from './client';

export interface CreditCard {
  id: number;
  name: string;
  creditLimit: number;
  closingDay: number;
  dueDay: number;
  currentInvoiceTotal: number;
}

export interface CreditCardInput {
  name: string;
  creditLimit: number;
  closingDay: number;
  dueDay: number;
}

export async function getCreditCards(): Promise<CreditCard[]> {
  const { data } = await api.get<CreditCard[]>('/api/credit-cards');
  return data;
}

export async function createCreditCard(input: CreditCardInput): Promise<CreditCard> {
  const { data } = await api.post<CreditCard>('/api/credit-cards', input);
  return data;
}

export async function updateCreditCard(id: number, input: CreditCardInput): Promise<CreditCard> {
  const { data } = await api.put<CreditCard>(`/api/credit-cards/${id}`, input);
  return data;
}

export async function deleteCreditCard(id: number): Promise<void> {
  await api.delete(`/api/credit-cards/${id}`);
}
