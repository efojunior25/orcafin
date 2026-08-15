import api from './client';

export type InvestmentType =
  | 'RENDA_FIXA'
  | 'TESOURO_DIRETO'
  | 'ACOES'
  | 'FUNDOS_IMOBILIARIOS'
  | 'CRIPTOMOEDAS'
  | 'FUNDOS'
  | 'OUTROS';

export interface Investment {
  id: string;
  name: string;
  type: InvestmentType;
  investedAmount: number;
  currentValue: number;
  notes: string | null;
}

export interface InvestmentInput {
  name: string;
  type: InvestmentType;
  investedAmount: number;
  currentValue: number;
  notes: string | null;
}

export async function getInvestments(): Promise<Investment[]> {
  const { data } = await api.get<Investment[]>('/api/investments');
  return data;
}

export async function createInvestment(input: InvestmentInput): Promise<Investment> {
  const { data } = await api.post<Investment>('/api/investments', input);
  return data;
}

export async function updateInvestment(id: string, input: InvestmentInput): Promise<Investment> {
  const { data } = await api.put<Investment>(`/api/investments/${id}`, input);
  return data;
}

export async function deleteInvestment(id: string): Promise<void> {
  await api.delete(`/api/investments/${id}`);
}
