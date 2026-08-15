import { useEffect, useMemo, useState } from 'react';
import * as investmentsApi from '../api/investments';
import type { Investment, InvestmentType } from '../api/investments';
import { formatCurrency, investmentTypeLabels, parseDecimal } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import './Accounts.css';

const investmentTypes: InvestmentType[] = [
  'RENDA_FIXA',
  'TESOURO_DIRETO',
  'ACOES',
  'FUNDOS_IMOBILIARIOS',
  'CRIPTOMOEDAS',
  'FUNDOS',
  'OUTROS',
];

function profitability(invested: number, current: number): number {
  if (invested <= 0) return 0;
  return ((current - invested) / invested) * 100;
}

export default function Investments() {
  const [investments, setInvestments] = useState<Investment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Investment | null>(null);
  const [name, setName] = useState('');
  const [type, setType] = useState<InvestmentType>('RENDA_FIXA');
  const [investedAmount, setInvestedAmount] = useState(0);
  const [investedAmountText, setInvestedAmountText] = useState('');
  const [currentValue, setCurrentValue] = useState(0);
  const [currentValueText, setCurrentValueText] = useState('');
  const [notes, setNotes] = useState('');
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await investmentsApi.getInvestments();
      setInvestments(data);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível carregar os investimentos.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const totals = useMemo(() => {
    const invested = investments.reduce((sum, i) => sum + i.investedAmount, 0);
    const current = investments.reduce((sum, i) => sum + i.currentValue, 0);
    return { invested, current };
  }, [investments]);

  function openCreate() {
    setEditing(null);
    setName('');
    setType('RENDA_FIXA');
    setInvestedAmount(0);
    setInvestedAmountText('');
    setCurrentValue(0);
    setCurrentValueText('');
    setNotes('');
    setModalOpen(true);
  }

  function openEdit(investment: Investment) {
    setEditing(investment);
    setName(investment.name);
    setType(investment.type);
    setInvestedAmount(investment.investedAmount);
    setInvestedAmountText(String(investment.investedAmount).replace('.', ','));
    setCurrentValue(investment.currentValue);
    setCurrentValueText(String(investment.currentValue).replace('.', ','));
    setNotes(investment.notes ?? '');
    setModalOpen(true);
  }

  async function handleSave() {
    if (!name.trim()) return;
    setSaving(true);
    try {
      const input = { name, type, investedAmount, currentValue, notes: notes.trim() || null };
      if (editing) {
        await investmentsApi.updateInvestment(editing.id, input);
      } else {
        await investmentsApi.createInvestment(input);
      }
      setModalOpen(false);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível salvar o investimento.'));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(investment: Investment) {
    if (!confirm(`Excluir o investimento "${investment.name}"?`)) return;
    try {
      await investmentsApi.deleteInvestment(investment.id);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível excluir o investimento.'));
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Investimentos</h1>
        <button className="btn btn-primary" onClick={openCreate}>
          + Novo Investimento
        </button>
      </div>

      {error && <div className="auth-error">{error}</div>}

      {!loading && investments.length > 0 && (
        <div className="card" style={{ marginBottom: 16, padding: 16 }}>
          <div className="recent-tx-meta">
            Investido: {formatCurrency(totals.invested)} · Valor atual: {formatCurrency(totals.current)} ·
            Rentabilidade: {profitability(totals.invested, totals.current).toFixed(2)}%
          </div>
        </div>
      )}

      {loading ? (
        <div className="empty-state">Carregando...</div>
      ) : investments.length === 0 ? (
        <div className="empty-state">Nenhum investimento cadastrado ainda.</div>
      ) : (
        <div className="accounts-grid">
          {investments.map((inv) => {
            const pct = profitability(inv.investedAmount, inv.currentValue);
            return (
              <div className="card account-card" key={inv.id}>
                <div className="account-card-top">
                  <div className="account-name">{inv.name}</div>
                  <span className="account-type-badge">{investmentTypeLabels[inv.type]}</span>
                </div>
                <div className={pct >= 0 ? 'account-balance text-success' : 'account-balance text-danger'}>
                  {formatCurrency(inv.currentValue)}
                </div>
                <div className="recent-tx-meta">
                  Investido {formatCurrency(inv.investedAmount)} · {pct >= 0 ? '+' : ''}
                  {pct.toFixed(2)}%
                </div>
                {inv.notes && <div className="recent-tx-meta">{inv.notes}</div>}
                <div className="account-actions">
                  <button className="btn btn-secondary" onClick={() => openEdit(inv)}>
                    Editar
                  </button>
                  <button className="btn btn-danger" onClick={() => handleDelete(inv)}>
                    Excluir
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="card modal-card" onClick={(e) => e.stopPropagation()}>
            <h2>{editing ? 'Editar Investimento' : 'Novo Investimento'}</h2>
            <div className="form-field">
              <label htmlFor="inv-name">Nome</label>
              <input
                id="inv-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Ex: Tesouro Selic 2029"
              />
            </div>
            <div className="form-field">
              <label htmlFor="inv-type">Tipo</label>
              <select id="inv-type" value={type} onChange={(e) => setType(e.target.value as InvestmentType)}>
                {investmentTypes.map((t) => (
                  <option key={t} value={t}>
                    {investmentTypeLabels[t]}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="inv-invested">Valor investido</label>
                <input
                  id="inv-invested"
                  type="text"
                  inputMode="decimal"
                  placeholder="0,00"
                  value={investedAmountText}
                  onChange={(e) => {
                    setInvestedAmountText(e.target.value);
                    setInvestedAmount(parseDecimal(e.target.value));
                  }}
                />
              </div>
              <div className="form-field">
                <label htmlFor="inv-current">Valor atual</label>
                <input
                  id="inv-current"
                  type="text"
                  inputMode="decimal"
                  placeholder="0,00"
                  value={currentValueText}
                  onChange={(e) => {
                    setCurrentValueText(e.target.value);
                    setCurrentValue(parseDecimal(e.target.value));
                  }}
                />
              </div>
            </div>
            <div className="form-field">
              <label htmlFor="inv-notes">Observações (opcional)</label>
              <input id="inv-notes" value={notes} onChange={(e) => setNotes(e.target.value)} />
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
