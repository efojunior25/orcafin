import api from './client';

export type CategoryType = 'RECEITA' | 'DESPESA';

export interface Category {
  id: string;
  name: string;
  icon: string;
  type: CategoryType;
  isDefault: boolean;
}

export interface CategoryInput {
  name: string;
  icon: string;
  type: CategoryType;
}

export async function getCategories(): Promise<Category[]> {
  const { data } = await api.get<Category[]>('/api/categories');
  return data;
}

export async function createCategory(input: CategoryInput): Promise<Category> {
  const { data } = await api.post<Category>('/api/categories', input);
  return data;
}

export async function deleteCategory(id: string): Promise<void> {
  await api.delete(`/api/categories/${id}`);
}
