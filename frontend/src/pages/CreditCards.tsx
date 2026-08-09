import { useEffect, useState } from 'react';
import * as creditCardsApi from '../api/creditCards';
import type { CreditCard } from '../api/creditCards';
import { formatCurrency, parseDecimal } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import './Accounts.css';

function limitStatus(used: number, limit: number): 'ok' | 'warn' | 'over' {
  if (limit <= 0) return 'ok';
  const pct = used / limit;
  if (pct >= 1) return 'over';
  if (pct >= 0.8) return 'warn';
  return 'ok';
}

export default function CreditCards() {
  const [cards, setCards] = useState<CreditCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<CreditCard | null>(null);
  const [name, setName] = useState('');
  const [creditLimit, setCreditLimit] = useState(0);
  const [creditLimitText, setCreditLimitText] = useState('');
  const [closingDay, setClosingDay] = useState(1);
  const [dueDay, setDueDay] = useState(10);
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await creditCardsApi.getCreditCards();
      setCards(data);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível carregar os cartões.'));
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
    setCreditLimit(0);
    setCreditLimitText('');
    setClosingDay(1);
    setDueDay(10);
    setModalOpen(true);
  }

  function openEdit(card: CreditCard) {
    setEditing(card);
    setName(card.name);
    setCreditLimit(card.creditLimit);
    setCreditLimitText(card.creditLimit ? String(card.creditLimit).replace('.', ',') : '');
    setClosingDay(card.closingDay);
    setDueDay(card.dueDay);
    setModalOpen(true);
  }

  async function handleSave() {
    if (!name.trim()) return;
    setSaving(true);
    try {
      const input = { name, creditLimit, closingDay, dueDay };
      if (editing) {
        await creditCardsApi.updateCreditCard(editing.id, input);
      } else {
        await creditCardsApi.createCreditCard(input);
      }
      setModalOpen(false);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível salvar o cartão.'));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(card: CreditCard) {
    if (!confirm(`Excluir o cartão "${card.name}"?`)) return;
    try {
      await creditCardsApi.deleteCreditCard(card.id);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível excluir o cartão.'));
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Cartões de Crédito</h1>
        <button className="btn btn-primary" onClick={openCreate}>
          + Novo Cartão
        </button>
      </div>

      {error && <div className="auth-error">{error}</div>}

      {loading ? (
        <div className="empty-state">Carregando...</div>
      ) : cards.length === 0 ? (
        <div className="empty-state">Nenhum cartão cadastrado ainda.</div>
      ) : (
        <div className="accounts-grid">
          {cards.map((card) => (
            <div className="card account-card" key={card.id}>
              <div className="account-card-top">
                <div className="account-name">{card.name}</div>
                <span className="account-type-badge">
                  Fecha dia {card.closingDay} · Vence dia {card.dueDay}
                </span>
              </div>
              <div className="account-balance text-danger">{formatCurrency(card.currentInvoiceTotal)}</div>
              <div className="recent-tx-meta">Fatura atual · Limite {formatCurrency(card.creditLimit)}</div>
              {card.creditLimit > 0 && (
                <>
                  <div className="limit-bar-track">
                    <div
                      className={`limit-bar-fill ${limitStatus(card.usedLimit, card.creditLimit)}`}
                      style={{ width: `${Math.min(100, (card.usedLimit / card.creditLimit) * 100)}%` }}
                    />
                  </div>
                  <div className="recent-tx-meta">
                    {formatCurrency(card.usedLimit)} usado de {formatCurrency(card.creditLimit)} (
                    {Math.round((card.usedLimit / card.creditLimit) * 100)}%)
                  </div>
                </>
              )}
              <div className="account-actions">
                <button className="btn btn-secondary" onClick={() => openEdit(card)}>
                  Editar
                </button>
                <button className="btn btn-danger" onClick={() => handleDelete(card)}>
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
            <h2>{editing ? 'Editar Cartão' : 'Novo Cartão'}</h2>
            <div className="form-field">
              <label htmlFor="cc-name">Nome</label>
              <input id="cc-name" value={name} onChange={(e) => setName(e.target.value)} placeholder="Ex: Nubank" />
            </div>
            <div className="form-field">
              <label htmlFor="cc-limit">Limite</label>
              <input
                id="cc-limit"
                type="text"
                inputMode="decimal"
                placeholder="0,00"
                value={creditLimitText}
                onChange={(e) => {
                  setCreditLimitText(e.target.value);
                  setCreditLimit(parseDecimal(e.target.value));
                }}
              />
            </div>
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="cc-closing">Dia de fechamento</label>
                <input
                  id="cc-closing"
                  type="number"
                  min="1"
                  max="28"
                  value={closingDay}
                  onChange={(e) => setClosingDay(Number(e.target.value))}
                />
              </div>
              <div className="form-field">
                <label htmlFor="cc-due">Dia de vencimento</label>
                <input
                  id="cc-due"
                  type="number"
                  min="1"
                  max="28"
                  value={dueDay}
                  onChange={(e) => setDueDay(Number(e.target.value))}
                />
              </div>
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
