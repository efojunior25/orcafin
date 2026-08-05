import api from './client';

export interface Budget {
  id: number;
  categoryId: number | null;
  categoryName: string;
  limitAmount: number;
  spentAmount: number;
  percentage: number;
}

export interface BudgetInput {
  categoryId: number | null;
  limitAmount: number;
}

export async function getBudgets(): Promise<Budget[]> {
  const { data } = await api.get<Budget[]>('/api/budgets');
  return data;
}

export async function createBudget(input: BudgetInput): Promise<Budget> {
  const { data } = await api.post<Budget>('/api/budgets', input);
  return data;
}

export async function updateBudget(id: number, input: BudgetInput): Promise<Budget> {
  const { data } = await api.put<Budget>(`/api/budgets/${id}`, input);
  return data;
}

export async function deleteBudget(id: number): Promise<void> {
  await api.delete(`/api/budgets/${id}`);
}
