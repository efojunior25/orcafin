import { useEffect, useMemo, useState } from 'react';
import * as budgetsApi from '../api/budgets';
import type { Budget } from '../api/budgets';
import * as categoriesApi from '../api/categories';
import type { Category } from '../api/categories';
import { formatCurrency } from '../utils/format';
import './Accounts.css';
import './Budgets.css';

export default function Budgets() {
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Budget | null>(null);
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [limitAmount, setLimitAmount] = useState(0);
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const [b, c] = await Promise.all([budgetsApi.getBudgets(), categoriesApi.getCategories()]);
      setBudgets(b);
      setCategories(c);
    } catch {
      setError('Não foi possível carregar os orçamentos.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const despesaCategories = useMemo(() => categories.filter((c) => c.type === 'DESPESA'), [categories]);
  const availableCategories = useMemo(
    () => despesaCategories.filter((c) => !budgets.some((b) => b.categoryId === c.id) || editing?.categoryId === c.id),
    [despesaCategories, budgets, editing]
  );
  const generalTaken = budgets.some((b) => b.categoryId === null) && editing?.categoryId !== null;

  function openCreate() {
    setEditing(null);
    setCategoryId(null);
    setLimitAmount(0);
    setModalOpen(true);
  }

  function openEdit(budget: Budget) {
    setEditing(budget);
    setCategoryId(budget.categoryId);
    setLimitAmount(budget.limitAmount);
    setModalOpen(true);
  }

  async function handleSave() {
    if (!limitAmount) return;
    setSaving(true);
    setError('');
    try {
      const input = { categoryId, limitAmount };
      if (editing) {
        await budgetsApi.updateBudget(editing.id, input);
      } else {
        await budgetsApi.createBudget(input);
      }
      setModalOpen(false);
      await load();
    } catch {
      setError('Não foi possível salvar o orçamento.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(budget: Budget) {
    if (!confirm(`Excluir o orçamento de "${budget.categoryName}"?`)) return;
    try {
      await budgetsApi.deleteBudget(budget.id);
      await load();
    } catch {
      setError('Não foi possível excluir o orçamento.');
    }
  }

  function barClass(percentage: number) {
    if (percentage >= 100) return 'budget-bar-fill over';
    if (percentage >= 80) return 'budget-bar-fill warn';
    return 'budget-bar-fill ok';
  }

  return (
    <div>
      <div className="page-header">
        <h1>Orçamento Mensal</h1>
        <button className="btn btn-primary" onClick={openCreate}>
          + Novo Orçamento
        </button>
      </div>

      {error && <div className="auth-error">{error}</div>}

      {loading ? (
        <div className="empty-state">Carregando...</div>
      ) : budgets.length === 0 ? (
        <div className="empty-state">Nenhum orçamento definido ainda.</div>
      ) : (
        <div className="budgets-list">
          {budgets.map((b) => (
            <div className="card budget-card" key={b.id}>
              <div className="budget-card-top">
                <div className="account-name">{b.categoryName}</div>
                <div className="budget-actions">
                  <button className="btn btn-secondary" onClick={() => openEdit(b)}>
                    Editar
                  </button>
                  <button className="btn btn-danger" onClick={() => handleDelete(b)}>
                    Excluir
                  </button>
                </div>
              </div>
              <div className="budget-bar-track">
                <div className={barClass(b.percentage)} style={{ width: `${Math.min(b.percentage, 100)}%` }} />
              </div>
              <div className="budget-values">
                <span className={b.percentage >= 100 ? 'text-danger' : 'text-secondary'}>
                  {formatCurrency(b.spentAmount)} de {formatCurrency(b.limitAmount)}
                </span>
                <span className={b.percentage >= 100 ? 'text-danger' : b.percentage >= 80 ? 'text-warning' : 'text-accent'}>
                  {b.percentage.toFixed(0)}%
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="card modal-card" onClick={(e) => e.stopPropagation()}>
            <h2>{editing ? 'Editar Orçamento' : 'Novo Orçamento'}</h2>
            <div className="form-field">
              <label>Categoria</label>
              <select
                value={categoryId ?? ''}
                onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : null)}
                disabled={!!editing}
              >
                {!generalTaken && <option value="">Geral (todas as despesas)</option>}
                {availableCategories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.icon} {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label>Limite mensal</label>
              <input
                type="number"
                min="0"
                step="0.01"
                value={limitAmount || ''}
                onChange={(e) => setLimitAmount(parseFloat(e.target.value) || 0)}
              />
            </div>
            <div className="modal-actions">
              <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>
                Cancelar
              </button>
              <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Salvando...' : 'Salvar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
