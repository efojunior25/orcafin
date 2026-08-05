import { useEffect, useState } from 'react';
import * as categoriesApi from '../api/categories';
import type { Category, CategoryType } from '../api/categories';
import './Categories.css';

export default function Categories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [name, setName] = useState('');
  const [icon, setIcon] = useState('');
  const [type, setType] = useState<CategoryType>('DESPESA');
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await categoriesApi.getCategories();
      setCategories(data);
    } catch {
      setError('Não foi possível carregar as categorias.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const receitas = categories.filter((c) => c.type === 'RECEITA');
  const despesas = categories.filter((c) => c.type === 'DESPESA');

  async function handleCreate() {
    if (!name.trim()) return;
    setSaving(true);
    try {
      await categoriesApi.createCategory({ name, icon: icon || '🏷️', type });
      setName('');
      setIcon('');
      await load();
    } catch {
      setError('Não foi possível criar a categoria.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(cat: Category) {
    if (!confirm(`Excluir a categoria "${cat.name}"?`)) return;
    try {
      await categoriesApi.deleteCategory(cat.id);
      await load();
    } catch {
      setError('Não foi possível excluir a categoria.');
    }
  }

  function renderColumn(title: string, list: Category[]) {
    return (
      <div className="category-column">
        <h2>{title}</h2>
        <div className="category-list">
          {list.length === 0 && <div className="empty-state">Nenhuma categoria.</div>}
          {list.map((cat) => (
            <div className="card category-row" key={cat.id}>
              <span className="category-icon">{cat.icon}</span>
              <span className="category-name">{cat.name}</span>
              {cat.isDefault ? (
                <span className="category-default-badge">Padrão</span>
              ) : (
                <button className="category-delete" onClick={() => handleDelete(cat)} title="Excluir">
                  ✕
                </button>
              )}
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <h1>Categorias</h1>
      </div>

      {error && <div className="auth-error">{error}</div>}

      {loading ? (
        <div className="empty-state">Carregando...</div>
      ) : (
        <div className="categories-columns">
          {renderColumn('Receitas', receitas)}
          {renderColumn('Despesas', despesas)}
        </div>
      )}

      <div className="card new-category-form">
        <h2>Nova Categoria</h2>
        <div className="new-category-fields">
          <div className="form-field">
            <label htmlFor="cat-name">Nome</label>
            <input id="cat-name" value={name} onChange={(e) => setName(e.target.value)} placeholder="Ex: Assinaturas" />
          </div>
          <div className="form-field">
            <label htmlFor="cat-icon">Ícone (emoji)</label>
            <input id="cat-icon" value={icon} onChange={(e) => setIcon(e.target.value)} placeholder="🎬" maxLength={4} />
          </div>
          <div className="form-field">
            <label htmlFor="cat-type">Tipo</label>
            <select id="cat-type" value={type} onChange={(e) => setType(e.target.value as CategoryType)}>
              <option value="DESPESA">Despesa</option>
              <option value="RECEITA">Receita</option>
            </select>
          </div>
          <button className="btn btn-primary" onClick={handleCreate} disabled={saving}>
            {saving ? 'Salvando...' : '+ Adicionar'}
          </button>
        </div>
      </div>
    </div>
  );
}
