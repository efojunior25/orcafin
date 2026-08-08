import api from './client';
import type { TransactionType } from './transactions';

export interface ParsedTransaction {
  type: TransactionType;
  amount: number;
  description: string;
  categoryId: string | null;
  categoryName: string | null;
  date: string;
  transcribedText: string | null;
}

export async function parseTransactionText(text: string): Promise<ParsedTransaction> {
  const { data } = await api.post<ParsedTransaction>('/api/transactions/parse', { text });
  return data;
}

export async function parseTransactionAudio(audioBlob: Blob): Promise<ParsedTransaction> {
  const formData = new FormData();
  formData.append('audio', audioBlob, 'audio.webm');

  const response = await fetch('/api/transactions/parse-audio', {
    method: 'POST',
    credentials: 'include',
    body: formData,
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    const message = errorBody?.message ?? 'Não foi possível transcrever o áudio.';
    throw new Error(message);
  }

  return response.json();
}
