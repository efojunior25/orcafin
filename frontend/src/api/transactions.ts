import api from './client';

export type TransactionType = 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA';
export type PaymentMethod = 'DINHEIRO' | 'DEBITO' | 'CREDITO' | 'PIX' | 'TRANSFERENCIA';
export type RecurrenceFrequency = 'DIARIA' | 'SEMANAL' | 'MENSAL' | 'ANUAL';
export type TransactionGroup = 'ASSINATURAS' | 'CONTAS_FIXAS_CASA' | 'COMPRAS_PONTUAIS' | 'DIVIDAS_PARCELAS' | 'OUTROS';

export interface Transaction {
  id: number;
  accountId: number | null;
  accountName: string | null;
  creditCardId: number | null;
  creditCardName: string | null;
  categoryId: number | null;
  categoryName: string | null;
  destinationAccountId: number | null;
  destinationAccountName: string | null;
  type: TransactionType;
  group: TransactionGroup | null;
  amount: number;
  description: string;
  date: string;
  paymentMethod: PaymentMethod;
  isRecurring: boolean;
  recurrenceFrequency: RecurrenceFrequency | null;
  recurrenceEndDate: string | null;
}

export interface TransactionInput {
  accountId: number | null;
  creditCardId: number | null;
  categoryId: number | null;
  destinationAccountId: number | null;
  type: TransactionType;
  group: TransactionGroup | null;
  amount: number;
  description: string;
  date: string;
  paymentMethod: PaymentMethod;
  isRecurring: boolean;
  recurrenceFrequency: RecurrenceFrequency | null;
  recurrenceEndDate: string | null;
}

export async function getTransactions(from?: string, to?: string): Promise<Transaction[]> {
  const params: Record<string, string> = {};
  if (from) params.from = from;
  if (to) params.to = to;
  const { data } = await api.get<Transaction[]>('/api/transactions', { params });
  return data;
}

export async function createTransaction(input: TransactionInput): Promise<Transaction> {
  const { data } = await api.post<Transaction>('/api/transactions', input);
  return data;
}

export async function updateTransaction(id: number, input: TransactionInput): Promise<Transaction> {
  const { data } = await api.put<Transaction>(`/api/transactions/${id}`, input);
  return data;
}

export async function deleteTransaction(id: number): Promise<void> {
  await api.delete(`/api/transactions/${id}`);
}
