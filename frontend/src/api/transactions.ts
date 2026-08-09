import api from './client';

export type TransactionType = 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA';
export type PaymentMethod = 'DINHEIRO' | 'DEBITO' | 'CREDITO' | 'PIX' | 'TRANSFERENCIA';
export type RecurrenceFrequency = 'DIARIA' | 'SEMANAL' | 'MENSAL' | 'ANUAL';
export type TransactionGroup = 'ASSINATURAS' | 'CONTAS_FIXAS_CASA' | 'COMPRAS_PONTUAIS' | 'DIVIDAS_PARCELAS' | 'OUTROS';

export interface Transaction {
  id: string;
  accountId: string | null;
  accountName: string | null;
  creditCardId: string | null;
  creditCardName: string | null;
  categoryId: string | null;
  categoryName: string | null;
  destinationAccountId: string | null;
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
  installmentNumber: number | null;
  installmentTotal: number | null;
}

export interface TransactionInput {
  accountId: string | null;
  creditCardId: string | null;
  categoryId: string | null;
  destinationAccountId: string | null;
  type: TransactionType;
  group: TransactionGroup | null;
  amount: number;
  description: string;
  date: string;
  paymentMethod: PaymentMethod;
  isRecurring: boolean;
  recurrenceFrequency: RecurrenceFrequency | null;
  recurrenceEndDate: string | null;
  installments?: number | null;
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

export async function updateTransaction(id: string, input: TransactionInput): Promise<Transaction> {
  const { data } = await api.put<Transaction>(`/api/transactions/${id}`, input);
  return data;
}

export async function deleteTransaction(id: string): Promise<void> {
  await api.delete(`/api/transactions/${id}`);
}
