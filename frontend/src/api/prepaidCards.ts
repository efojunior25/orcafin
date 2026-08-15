import api from './client';

export type PrepaidCardType =
  | 'VALE_ALIMENTACAO'
  | 'VALE_REFEICAO'
  | 'USO_LIVRE'
  | 'MOBILIDADE'
  | 'VALE_TRANSPORTE';

export type TransitSubtype = 'URBANO' | 'INTERMUNICIPAL' | 'METRO' | 'ESTUDANTE_MEIA_PASSAGEM';

export interface PrepaidCard {
  id: string;
  name: string;
  type: PrepaidCardType;
  subtype: TransitSubtype | null;
  balance: number;
  rechargeDay: number | null;
  rechargeAmount: number | null;
}

export interface PrepaidCardInput {
  name: string;
  type: PrepaidCardType;
  subtype: TransitSubtype | null;
  rechargeDay: number | null;
  rechargeAmount: number | null;
}

export async function getPrepaidCards(): Promise<PrepaidCard[]> {
  const { data } = await api.get<PrepaidCard[]>('/api/prepaid-cards');
  return data;
}

export async function createPrepaidCard(input: PrepaidCardInput): Promise<PrepaidCard> {
  const { data } = await api.post<PrepaidCard>('/api/prepaid-cards', input);
  return data;
}

export async function updatePrepaidCard(id: string, input: PrepaidCardInput): Promise<PrepaidCard> {
  const { data } = await api.put<PrepaidCard>(`/api/prepaid-cards/${id}`, input);
  return data;
}

export async function deletePrepaidCard(id: string): Promise<void> {
  await api.delete(`/api/prepaid-cards/${id}`);
}
