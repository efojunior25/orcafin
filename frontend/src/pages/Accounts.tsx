import { useEffect, useState } from 'react';
import * as accountsApi from '../api/accounts';
import type { Account, AccountType } from '../api/accounts';
import { formatCurrency, accountTypeLabels } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import './Accounts.css';

const typeOptions: AccountType[] = ['CORRENTE', 'POUPANCA', 'CARTEIRA'];

export default function Accounts() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Account | null>(null);
  const [name, setName] = useState('');
  const [type, setType] = useState<AccountType>('CORRENTE');
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await accountsApi.getAccounts();
      setAccounts(data);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível carregar as contas.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  function openCreate() {
    setEditing(null);
    setName('');
    setType('CORRENTE');
    setModalOpen(true);
  }

  function openEdit(acc: Account) {
    setEditing(acc);
    setName(acc.name);
    setType(acc.type);
    setModalOpen(true);
  }

  async function handleSave() {
    if (!name.trim()) return;
    setSaving(true);
    try {
      if (editing) {
        await accountsApi.updateAccount(editing.id, { name, type });
      } else {
        await accountsApi.createAccount({ name, type });
      }
      setModalOpen(false);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível salvar a conta.'));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(acc: Account) {
    if (!confirm(`Excluir a conta "${acc.name}"?`)) return;
    try {
      await accountsApi.deleteAccount(acc.id);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível excluir a conta.'));
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Contas</h1>
        <button className="btn btn-primary" onClick={openCreate}>
          + Nova Conta
        </button>
      </div>

      {error && <div className="auth-error">{error}</div>}

      {loading ? (
        <div className="empty-state">Carregando...</div>
      ) : accounts.length === 0 ? (
        <div className="empty-state">Nenhuma conta cadastrada ainda.</div>
      ) : (
        <div className="accounts-grid">
          {accounts.map((acc) => (
            <div className="card account-card" key={acc.id}>
              <div className="account-card-top">
                <div className="account-name">{acc.name}</div>
                <span className="account-type-badge">{accountTypeLabels[acc.type] ?? acc.type}</span>
              </div>
              <div className={`account-balance ${acc.balance < 0 ? 'text-danger' : 'text-accent'}`}>
                {formatCurrency(acc.balance)}
              </div>
              <div className="account-actions">
                <button className="btn btn-secondary" onClick={() => openEdit(acc)}>
                  Editar
                </button>
                <button className="btn btn-danger" onClick={() => handleDelete(acc)}>
                  Excluir
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="card modal-card" onClick={(e) => e.stopPropagation()}>
            <h2>{editing ? 'Editar Conta' : 'Nova Conta'}</h2>
            <div className="form-field">
              <label htmlFor="acc-name">Nome</label>
              <input id="acc-name" value={name} onChange={(e) => setName(e.target.value)} placeholder="Ex: Nubank" />
            </div>
            <div className="form-field">
              <label htmlFor="acc-type">Tipo</label>
              <select id="acc-type" value={type} onChange={(e) => setType(e.target.value as AccountType)}>
                {typeOptions.map((t) => (
                  <option key={t} value={t}>
                    {accountTypeLabels[t]}
                  </option>
                ))}
              </select>
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
