import { useEffect, useState } from 'react';
import * as vaultsApi from '../api/vaults';
import type { Vault, VaultMovement } from '../api/vaults';
import { formatCurrency, formatDate, parseDecimal, vaultMovementTypeLabels } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import './Accounts.css';

export default function Vaults() {
  const [vaults, setVaults] = useState<Vault[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Vault | null>(null);
  const [name, setName] = useState('');
  const [annualRate, setAnnualRate] = useState(0);
  const [annualRateText, setAnnualRateText] = useState('');
  const [saving, setSaving] = useState(false);

  const [movementsVault, setMovementsVault] = useState<Vault | null>(null);
  const [movements, setMovements] = useState<VaultMovement[]>([]);
  const [movementsLoading, setMovementsLoading] = useState(false);

  const [movingVault, setMovingVault] = useState<Vault | null>(null);
  const [movementKind, setMovementKind] = useState<'deposit' | 'withdraw'>('deposit');
  const [amountText, setAmountText] = useState('');
  const [movementSaving, setMovementSaving] = useState(false);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await vaultsApi.getVaults();
      setVaults(data);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível carregar as caixinhas.'));
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
    setAnnualRate(0);
    setAnnualRateText('');
    setModalOpen(true);
  }

  function openEdit(vault: Vault) {
    setEditing(vault);
    setName(vault.name);
    setAnnualRate(vault.annualRate);
    setAnnualRateText(vault.annualRate ? String(vault.annualRate).replace('.', ',') : '');
    setModalOpen(true);
  }

  async function handleSave() {
    if (!name.trim()) return;
    setSaving(true);
    try {
      const input = { name, annualRate };
      if (editing) {
        await vaultsApi.updateVault(editing.id, input);
      } else {
        await vaultsApi.createVault(input);
      }
      setModalOpen(false);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível salvar a caixinha.'));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(vault: Vault) {
    if (!confirm(`Excluir a caixinha "${vault.name}"?`)) return;
    try {
      await vaultsApi.deleteVault(vault.id);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível excluir a caixinha.'));
    }
  }

  async function openMovements(vault: Vault) {
    setMovementsVault(vault);
    setMovementsLoading(true);
    try {
      const data = await vaultsApi.getVaultMovements(vault.id);
      setMovements(data);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível carregar o extrato.'));
    } finally {
      setMovementsLoading(false);
    }
  }

  function openMove(vault: Vault, kind: 'deposit' | 'withdraw') {
    setMovingVault(vault);
    setMovementKind(kind);
    setAmountText('');
  }

  async function handleMove() {
    if (!movingVault) return;
    const amount = parseDecimal(amountText);
    if (!amount) return;
    setMovementSaving(true);
    setError('');
    try {
      if (movementKind === 'deposit') {
        await vaultsApi.depositVault(movingVault.id, amount);
      } else {
        await vaultsApi.withdrawVault(movingVault.id, amount);
      }
      setMovingVault(null);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível registrar o movimento.'));
    } finally {
      setMovementSaving(false);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Caixinhas</h1>
        <button className="btn btn-primary" onClick={openCreate}>
          + Nova Caixinha
        </button>
      </div>

      {error && <div className="auth-error">{error}</div>}

      {loading ? (
        <div className="empty-state">Carregando...</div>
      ) : vaults.length === 0 ? (
        <div className="empty-state">Nenhuma caixinha cadastrada ainda.</div>
      ) : (
        <div className="accounts-grid">
          {vaults.map((vault) => (
            <div className="card account-card" key={vault.id}>
              <div className="account-card-top">
                <div className="account-name">{vault.name}</div>
                <span className="account-type-badge">{vault.annualRate}% a.a.</span>
              </div>
              <div className="account-balance">{formatCurrency(vault.balance)}</div>
              <div className="recent-tx-meta">Saldo com rendimento diário automático</div>
              <div className="account-actions">
                <button className="btn btn-secondary" onClick={() => openMove(vault, 'deposit')}>
                  Aportar
                </button>
                <button className="btn btn-secondary" onClick={() => openMove(vault, 'withdraw')}>
                  Resgatar
                </button>
              </div>
              <div className="account-actions">
                <button className="btn btn-ghost" onClick={() => openMovements(vault)}>
                  Extrato
                </button>
                <button className="btn btn-secondary" onClick={() => openEdit(vault)}>
                  Editar
                </button>
                <button className="btn btn-danger" onClick={() => handleDelete(vault)}>
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
            <h2>{editing ? 'Editar Caixinha' : 'Nova Caixinha'}</h2>
            <div className="form-field">
              <label htmlFor="vault-name">Nome</label>
              <input
                id="vault-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Ex: Reserva de emergência"
              />
            </div>
            <div className="form-field">
              <label htmlFor="vault-rate">Taxa de rendimento (% ao ano)</label>
              <input
                id="vault-rate"
                type="text"
                inputMode="decimal"
                placeholder="0,00"
                value={annualRateText}
                onChange={(e) => {
                  setAnnualRateText(e.target.value);
                  setAnnualRate(parseDecimal(e.target.value));
                }}
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

      {movingVault && (
        <div className="modal-overlay" onClick={() => setMovingVault(null)}>
          <div className="card modal-card" onClick={(e) => e.stopPropagation()}>
            <h2>{movementKind === 'deposit' ? 'Aportar' : 'Resgatar'} — {movingVault.name}</h2>
            <div className="form-field">
              <label htmlFor="move-amount">Valor</label>
              <input
                id="move-amount"
                type="text"
                inputMode="decimal"
                placeholder="0,00"
                value={amountText}
                onChange={(e) => setAmountText(e.target.value)}
              />
            </div>
            <div className="modal-actions">
              <button className="btn btn-secondary" onClick={() => setMovingVault(null)}>
                Cancelar
              </button>
              <button className="btn btn-primary" onClick={handleMove} disabled={movementSaving}>
                {movementSaving ? 'Salvando...' : 'Confirmar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {movementsVault && (
        <div className="modal-overlay" onClick={() => setMovementsVault(null)}>
          <div className="card modal-card" onClick={(e) => e.stopPropagation()}>
            <h2>Extrato — {movementsVault.name}</h2>
            {movementsLoading ? (
              <div className="empty-state">Carregando...</div>
            ) : movements.length === 0 ? (
              <div className="empty-state">Nenhum movimento ainda.</div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Tipo</th>
                    <th>Valor</th>
                  </tr>
                </thead>
                <tbody>
                  {movements.map((m) => (
                    <tr key={m.id}>
                      <td>{formatDate(m.date)}</td>
                      <td>{vaultMovementTypeLabels[m.type]}</td>
                      <td>{formatCurrency(m.amount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <div className="modal-actions">
              <button className="btn btn-secondary" onClick={() => setMovementsVault(null)}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
