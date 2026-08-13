import api from './client';

export type SuggestionType = 'ADD' | 'REMOVE' | 'FIX_DATE' | 'MOVE_ACCOUNT';
export type TransactionType = 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA';

export interface Suggestion {
  id: string;
  type: SuggestionType;
  reason: string;
  date: string;
  description: string;
  amount: number;
  transactionType: TransactionType;
  existingTransactionId: string | null;
  currentDate: string | null;
  currentAccountName: string | null;
}

export interface ImportStatementResult {
  sessionId: string;
  suggestions: Suggestion[];
}

export interface ApplyResult {
  applied: number;
  message: string;
}

export async function importStatement(
  file: File,
  target: { accountId?: string; creditCardId?: string }
): Promise<ImportStatementResult> {
  const formData = new FormData();
  formData.append('file', file);
  if (target.accountId) formData.append('accountId', target.accountId);
  if (target.creditCardId) formData.append('creditCardId', target.creditCardId);

  const { data } = await api.post<ImportStatementResult>('/api/statements/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

export async function applySuggestions(sessionId: string, approvedSuggestionIds: string[]): Promise<ApplyResult> {
  const { data } = await api.post<ApplyResult>(`/api/statements/${sessionId}/apply`, { approvedSuggestionIds });
  return data;
}
