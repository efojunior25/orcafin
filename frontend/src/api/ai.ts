import api from './client';
import type { TransactionType } from './transactions';

export interface ParsedTransaction {
  type: TransactionType;
  amount: number;
  description: string;
  categoryId: number | null;
  categoryName: string | null;
  date: string;
}

export async function parseTransactionText(text: string): Promise<ParsedTransaction> {
  const { data } = await api.post<ParsedTransaction>('/api/transactions/parse', { text });
  return data;
}
