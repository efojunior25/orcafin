import { useEffect, useState } from 'react';
import * as prepaidCardsApi from '../api/prepaidCards';
import type { PrepaidCard, PrepaidCardType, TransitSubtype } from '../api/prepaidCards';
import { formatCurrency, parseDecimal, prepaidCardTypeLabels, transitSubtypeLabels } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import './Accounts.css';

const prepaidCardTypes: PrepaidCardType[] = [
  'VALE_ALIMENTACAO',
  'VALE_REFEICAO',
  'USO_LIVRE',
  'MOBILIDADE',
  'VALE_TRANSPORTE',
];

const transitSubtypes: TransitSubtype[] = ['URBANO', 'INTERMUNICIPAL', 'METRO', 'ESTUDANTE_MEIA_PASSAGEM'];

export default function PrepaidCards() {
  const [cards, setCards] = useState<PrepaidCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PrepaidCard | null>(null);
  const [name, setName] = useState('');
  const [type, setType] = useState<PrepaidCardType>('VALE_ALIMENTACAO');
  const [subtype, setSubtype] = useState<TransitSubtype>('URBANO');
  const [rechargeDay, setRechargeDay] = useState<number | ''>('');
  const [rechargeAmount, setRechargeAmount] = useState(0);
  const [rechargeAmountText, setRechargeAmountText] = useState('');
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await prepaidCardsApi.getPrepaidCards();
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
    setType('VALE_ALIMENTACAO');
    setSubtype('URBANO');
    setRechargeDay('');
    setRechargeAmount(0);
    setRechargeAmountText('');
    setModalOpen(true);
  }

  function openEdit(card: PrepaidCard) {
    setEditing(card);
    setName(card.name);
    setType(card.type);
    setSubtype(card.subtype ?? 'URBANO');
    setRechargeDay(card.rechargeDay ?? '');
    setRechargeAmount(card.rechargeAmount ?? 0);
    setRechargeAmountText(card.rechargeAmount ? String(card.rechargeAmount).replace('.', ',') : '');
    setModalOpen(true);
  }

  async function handleSave() {
    if (!name.trim()) return;
    setSaving(true);
    try {
      const input = {
        name,
        type,
        subtype: type === 'VALE_TRANSPORTE' ? subtype : null,
        rechargeDay: rechargeDay === '' ? null : rechargeDay,
        rechargeAmount: rechargeDay === '' ? null : rechargeAmount,
      };
      if (editing) {
        await prepaidCardsApi.updatePrepaidCard(editing.id, input);
      } else {
        await prepaidCardsApi.createPrepaidCard(input);
      }
      setModalOpen(false);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível salvar o cartão.'));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(card: PrepaidCard) {
    if (!confirm(`Excluir o cartão "${card.name}"?`)) return;
    try {
      await prepaidCardsApi.deletePrepaidCard(card.id);
      await load();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível excluir o cartão.'));
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Cartões Pré-pagos</h1>
        <button className="btn btn-primary" onClick={openCreate}>
          + Novo Cartão
        </button>
      </div>

      {error && <div className="auth-error">{error}</div>}

      {loading ? (
        <div className="empty-state">Carregando...</div>
      ) : cards.length === 0 ? (
        <div className="empty-state">Nenhum cartão pré-pago cadastrado ainda.</div>
      ) : (
        <div className="accounts-grid">
          {cards.map((card) => (
            <div className="card account-card" key={card.id}>
              <div className="account-card-top">
                <div className="account-name">{card.name}</div>
                <span className="account-type-badge">
                  {prepaidCardTypeLabels[card.type]}
                  {card.subtype ? ` · ${transitSubtypeLabels[card.subtype]}` : ''}
                </span>
              </div>
              <div className="account-balance">{formatCurrency(card.balance)}</div>
              <div className="recent-tx-meta">
                Saldo disponível
                {card.rechargeDay && card.rechargeAmount
                  ? ` · Recarga dia ${card.rechargeDay} de ${formatCurrency(card.rechargeAmount)}`
                  : ''}
              </div>
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
              <label htmlFor="pc-name">Nome</label>
              <input
                id="pc-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Ex: VR Sodexo"
              />
            </div>
            <div className="form-field">
              <label htmlFor="pc-type">Tipo</label>
              <select id="pc-type" value={type} onChange={(e) => setType(e.target.value as PrepaidCardType)}>
                {prepaidCardTypes.map((t) => (
                  <option key={t} value={t}>
                    {prepaidCardTypeLabels[t]}
                  </option>
                ))}
              </select>
            </div>
            {type === 'VALE_TRANSPORTE' && (
              <div className="form-field">
                <label htmlFor="pc-subtype">Modalidade</label>
                <select
                  id="pc-subtype"
                  value={subtype}
                  onChange={(e) => setSubtype(e.target.value as TransitSubtype)}
                >
                  {transitSubtypes.map((s) => (
                    <option key={s} value={s}>
                      {transitSubtypeLabels[s]}
                    </option>
                  ))}
                </select>
              </div>
            )}
            <div className="form-row">
              <div className="form-field">
                <label htmlFor="pc-recharge-day">Dia da recarga (opcional)</label>
                <input
                  id="pc-recharge-day"
                  type="number"
                  min="1"
                  max="28"
                  placeholder="Ex: 5"
                  value={rechargeDay}
                  onChange={(e) => setRechargeDay(e.target.value === '' ? '' : Number(e.target.value))}
                />
              </div>
              <div className="form-field">
                <label htmlFor="pc-recharge-amount">Valor da recarga</label>
                <input
                  id="pc-recharge-amount"
                  type="text"
                  inputMode="decimal"
                  placeholder="0,00"
                  value={rechargeAmountText}
                  onChange={(e) => {
                    setRechargeAmountText(e.target.value);
                    setRechargeAmount(parseDecimal(e.target.value));
                  }}
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
