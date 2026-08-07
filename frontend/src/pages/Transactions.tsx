import { useEffect, useMemo, useState } from 'react';
import * as transactionsApi from '../api/transactions';
import type { Transaction, TransactionInput, TransactionType, PaymentMethod, RecurrenceFrequency, TransactionGroup } from '../api/transactions';
import * as accountsApi from '../api/accounts';
import type { Account } from '../api/accounts';
import * as creditCardsApi from '../api/creditCards';
import type { CreditCard } from '../api/creditCards';
import * as categoriesApi from '../api/categories';
import type { Category } from '../api/categories';
import * as aiApi from '../api/ai';
import { formatCurrency, formatDate, monthRange, todayISO, paymentMethodLabels, recurrenceFrequencyLabels, transactionGroupLabels } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import './Accounts.css';
import './Transactions.css';

const paymentMethods: PaymentMethod[] = ['DINHEIRO', 'DEBITO', 'CREDITO', 'PIX', 'TRANSFERENCIA'];
const recurrenceFrequencies: RecurrenceFrequency[] = ['DIARIA', 'SEMANAL', 'MENSAL', 'ANUAL'];
const transactionGroups: TransactionGroup[] = ['ASSINATURAS', 'CONTAS_FIXAS_CASA', 'COMPRAS_PONTUAIS', 'DIVIDAS_PARCELAS', 'OUTROS'];

function emptyForm(accounts: Account[], categories: Category[]): TransactionInput {
  const despesaCategories = categories.filter((c) => c.type === 'DESPESA');
  return {
    accountId: accounts[0]?.id ?? 0,
    creditCardId: null,
    categoryId: despesaCategories[0]?.id ?? 0,
    destinationAccountId: null,
    type: 'DESPESA',
    group: null,
    amount: 0,
    description: '',
    date: todayISO(),
    paymentMethod: 'DEBITO',
    isRecurring: false,
    recurrenceFrequency: null,
    recurrenceEndDate: null,
  };
}

export default function Transactions() {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [typeFilter, setTypeFilter] = useState<'ALL' | TransactionType>('ALL');

  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Transaction | null>(null);
  const [form, setForm] = useState<TransactionInput>(emptyForm([], []));
  const [saving, setSaving] = useState(false);

  const [aiText, setAiText] = useState('');
  const [aiLoading, setAiLoading] = useState(false);

  async function loadAll() {
    setLoading(true);
    setError('');
    try {
      const { from, to } = monthRange(year, month);
      const [tx, acc, cc, cat] = await Promise.all([
        transactionsApi.getTransactions(from, to),
        accountsApi.getAccounts(),
        creditCardsApi.getCreditCards(),
        categoriesApi.getCategories(),
      ]);
      setTransactions(tx);
      setAccounts(acc);
      setCreditCards(cc);
      setCategories(cat);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível carregar as transações.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [year, month]);

  const filtered = useMemo(() => {
    if (typeFilter === 'ALL') return transactions;
    return transactions.filter((t) => t.type === typeFilter);
  }, [transactions, typeFilter]);

  const categoriesForType = useMemo(
    () => categories.filter((c) => c.type === form.type),
    [categories, form.type]
  );

  function openCreate() {
    setEditing(null);
    setForm(emptyForm(accounts, categories));
    setModalOpen(true);
  }

  async function handleAiParse() {
    if (!aiText.trim() || accounts.length === 0) return;
    setAiLoading(true);
    setError('');
    try {
      const parsed = await aiApi.parseTransactionText(aiText.trim());
      setEditing(null);
      setForm({
        accountId: accounts[0]?.id ?? null,
        creditCardId: null,
        categoryId: parsed.categoryId,
        destinationAccountId: null,
        type: parsed.type,
        group: null,
        amount: parsed.amount,
        description: parsed.description,
        date: parsed.date,
        paymentMethod: 'DEBITO',
        isRecurring: false,
        recurrenceFrequency: null,
        recurrenceEndDate: null,
      });
      setAiText('');
      setModalOpen(true);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível interpretar o texto com a IA local.'));
    } finally {
      setAiLoading(false);
    }
  }

  function openEdit(tx: Transaction) {
    setEditing(tx);
    setForm({
      accountId: tx.accountId,
      creditCardId: tx.creditCardId,
      categoryId: tx.categoryId,
      destinationAccountId: tx.destinationAccountId,
      type: tx.type,
      group: tx.group,
      amount: tx.amount,
      description: tx.description,
      date: tx.date,
      paymentMethod: tx.paymentMethod,
      isRecurring: tx.isRecurring,
      recurrenceFrequency: tx.recurrenceFrequency,
      recurrenceEndDate: tx.recurrenceEndDate,
    });
    setModalOpen(true);
  }

  function setType(type: TransactionType) {
    if (type === 'TRANSFERENCIA') {
      const accountId = form.accountId ?? accounts[0]?.id ?? null;
      const otherAccount = accounts.find((a) => a.id !== accountId);
      setForm((f) => ({
        ...f,
        type,
        accountId,
        creditCardId: null,
        categoryId: null,
        destinationAccountId: otherAccount?.id ?? null,
      }));
      return;
    }
    const firstCat = categories.find((c) => c.type === type);
    const accountId = type === 'RECEITA' ? form.accountId ?? accounts[0]?.id ?? 0 : form.accountId;
    setForm((f) => ({
      ...f,
      type,
      accountId: type === 'RECEITA' ? accountId : f.accountId,
      creditCardId: type === 'RECEITA' ? null : f.creditCardId,
      categoryId: firstCat?.id ?? 0,
      destinationAccountId: null,
    }));
  }

  function setPaymentSource(source: 'CONTA' | 'CARTAO') {
    if (source === 'CARTAO') {
      setForm((f) => ({ ...f, accountId: null, creditCardId: f.creditCardId ?? creditCards[0]?.id ?? null }));
    } else {
      setForm((f) => ({ ...f, creditCardId: null, accountId: f.accountId ?? accounts[0]?.id ?? null }));
    }
  }

  async function handleSave() {
    const isTransfer = form.type === 'TRANSFERENCIA';
    if ((!form.accountId && !form.creditCardId) || !form.amount || !form.date) {
      setError('Preencha todos os campos obrigatórios.');
      return;
    }
    if (isTransfer && (!form.destinationAccountId || form.destinationAccountId === form.accountId)) {
      setError('Escolha uma conta de destino diferente da conta de origem.');
      return;
    }
    if (!isTransfer && !form.categoryId) {
      setError('Preencha todos os campos obrigatórios.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      if (editing) {
        await transactionsApi.updateTransaction(editing.id, form);
      } else {
        await transactionsApi.createTransaction(form);
      }
      setModalOpen(false);
      await loadAll();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível salvar a transação.'));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(tx: Transaction) {
    if (!confirm(`Excluir a transação "${tx.description}"?`)) return;
    try {
      await transactionsApi.deleteTransaction(tx.id);
      await loadAll();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível excluir a transação.'));
    }
  }

  const monthOptions = Array.from({ length: 12 }, (_, i) => i + 1);
  const yearOptions = Array.from({ length: 6 }, (_, i) => now.getFullYear() - 3 + i);

  return (
    <div>
      <div className="page-header">
        <h1>Transações</h1>
        <button className="btn btn-primary" onClick={openCreate} disabled={accounts.length === 0}>
          + Nova Transação
        </button>
      </div>

      {accounts.length === 0 && !loading && (
        <div className="auth-error">Cadastre ao menos uma conta antes de lançar transações.</div>
      )}
      {error && <div className="auth-error">{error}</div>}

      <div className="card ai-quick-entry">
        <label htmlFor="ai-text">Lançamento rápido com IA local</label>
        <div className="ai-quick-entry-row">
          <input
            id="ai-text"
            value={aiText}
            onChange={(e) => setAiText(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleAiParse()}
            placeholder="Ex: Pizza 59,90"
            disabled={aiLoading || accounts.length === 0}
          />
          <button
            className="btn btn-secondary"
            onClick={handleAiParse}
            disabled={aiLoading || !aiText.trim() || accounts.length === 0}
          >
            {aiLoading ? 'Interpretando...' : 'Interpretar'}
          </button>
        </div>
        <div className="ai-quick-entry-hint">
          Digite algo como "Pizza 59,90" e revise antes de salvar. Requer o container Ollama rodando.
        </div>
      </div>

      <div className="filters-bar">
        <select value={month} onChange={(e) => setMonth(Number(e.target.value))}>
          {monthOptions.map((m) => (
            <option key={m} value={m}>
              {new Date(2000, m - 1, 1).toLocaleDateString('pt-BR', { month: 'long' })}
            </option>
          ))}
        </select>
        <select value={year} onChange={(e) => setYear(Number(e.target.value))}>
          {yearOptions.map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </select>
        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value as any)}>
          <option value="ALL">Todos os tipos</option>
          <option value="RECEITA">Receitas</option>
          <option value="DESPESA">Despesas</option>
          <option value="TRANSFERENCIA">Transferências</option>
        </select>
      </div>

      {loading ? (
        <div className="empty-state">Carregando...</div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">Nenhuma transação neste período.</div>
      ) : (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Data</th>
                <th>Descrição</th>
                <th>Categoria</th>
                <th>Grupo</th>
                <th>Conta</th>
                <th>Pagamento</th>
                <th>Valor</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((tx) => (
                <tr key={tx.id}>
                  <td>{formatDate(tx.date)}</td>
                  <td>
                    {tx.description}
                    {tx.isRecurring && <span className="recurring-badge">↻ {recurrenceFrequencyLabels[tx.recurrenceFrequency ?? ''] ?? ''}</span>}
                  </td>
                  <td>{tx.type === 'TRANSFERENCIA' ? 'Transferência' : tx.categoryName}</td>
                  <td>{tx.group ? transactionGroupLabels[tx.group] : '—'}</td>
                  <td>
                    {tx.type === 'TRANSFERENCIA'
                      ? `${tx.accountName} → ${tx.destinationAccountName}`
                      : tx.creditCardName ?? tx.accountName}
                  </td>
                  <td>{paymentMethodLabels[tx.paymentMethod] ?? tx.paymentMethod}</td>
                  <td
                    className={
                      tx.type === 'RECEITA'
                        ? 'tx-amount-receita'
                        : tx.type === 'DESPESA'
                          ? 'tx-amount-despesa'
                          : 'tx-amount-transferencia'
                    }
                  >
                    {tx.type === 'RECEITA' ? '+' : tx.type === 'DESPESA' ? '-' : '⇄'} {formatCurrency(tx.amount)}
                  </td>
                  <td>
                    <div className="tx-actions">
                      <button className="btn btn-secondary" onClick={() => openEdit(tx)}>
                        Editar
                      </button>
                      <button className="btn btn-danger" onClick={() => handleDelete(tx)}>
                        Excluir
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="card modal-card wide" onClick={(e) => e.stopPropagation()}>
            <h2>{editing ? 'Editar Transação' : 'Nova Transação'}</h2>

            <div className="type-toggle">
              <button
                type="button"
                className={form.type === 'RECEITA' ? 'active-receita' : ''}
                onClick={() => setType('RECEITA')}
              >
                Receita
              </button>
              <button
                type="button"
                className={form.type === 'DESPESA' ? 'active-despesa' : ''}
                onClick={() => setType('DESPESA')}
              >
                Despesa
              </button>
              <button
                type="button"
                className={form.type === 'TRANSFERENCIA' ? 'active-transferencia' : ''}
                onClick={() => setType('TRANSFERENCIA')}
              >
                Transferência
              </button>
            </div>

            <div className="form-row">
              <div className="form-field">
                <label>Valor</label>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.amount || ''}
                  onChange={(e) => setForm((f) => ({ ...f, amount: parseFloat(e.target.value) || 0 }))}
                />
              </div>
              <div className="form-field">
                <label>Data</label>
                <input type="date" value={form.date} onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))} />
              </div>
            </div>

            <div className="form-field">
              <label>Descrição</label>
              <input
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                placeholder="Ex: Supermercado"
              />
            </div>

            {form.type === 'DESPESA' && (
              <div className="type-toggle">
                <button
                  type="button"
                  className={!form.creditCardId ? 'active-despesa' : ''}
                  onClick={() => setPaymentSource('CONTA')}
                >
                  Conta
                </button>
                <button
                  type="button"
                  className={form.creditCardId ? 'active-despesa' : ''}
                  onClick={() => setPaymentSource('CARTAO')}
                  disabled={creditCards.length === 0}
                >
                  Cartão de Crédito
                </button>
              </div>
            )}

            <div className="form-row">
              {form.type === 'DESPESA' && form.creditCardId ? (
                <div className="form-field">
                  <label>Cartão</label>
                  <select
                    value={form.creditCardId}
                    onChange={(e) => setForm((f) => ({ ...f, creditCardId: Number(e.target.value) }))}
                  >
                    {creditCards.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
              ) : (
                <div className="form-field">
                  <label>{form.type === 'TRANSFERENCIA' ? 'Conta de origem' : 'Conta'}</label>
                  <select
                    value={form.accountId ?? ''}
                    onChange={(e) => {
                      const accountId = Number(e.target.value);
                      setForm((f) => ({
                        ...f,
                        accountId,
                        destinationAccountId:
                          f.type === 'TRANSFERENCIA' && f.destinationAccountId === accountId
                            ? null
                            : f.destinationAccountId,
                      }));
                    }}
                  >
                    {accounts.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
              {form.type === 'TRANSFERENCIA' ? (
                <div className="form-field">
                  <label>Conta de destino</label>
                  <select
                    value={form.destinationAccountId ?? ''}
                    onChange={(e) => setForm((f) => ({ ...f, destinationAccountId: Number(e.target.value) }))}
                  >
                    <option value="" disabled>
                      Selecione...
                    </option>
                    {accounts
                      .filter((a) => a.id !== form.accountId)
                      .map((a) => (
                        <option key={a.id} value={a.id}>
                          {a.name}
                        </option>
                      ))}
                  </select>
                </div>
              ) : (
                <div className="form-field">
                  <label>Categoria</label>
                  <select
                    value={form.categoryId ?? ''}
                    onChange={(e) => setForm((f) => ({ ...f, categoryId: Number(e.target.value) }))}
                  >
                    {categoriesForType.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.icon} {c.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>

            <div className="form-field">
              <label>Forma de pagamento</label>
              <select
                value={form.paymentMethod}
                onChange={(e) => setForm((f) => ({ ...f, paymentMethod: e.target.value as PaymentMethod }))}
              >
                {paymentMethods.map((p) => (
                  <option key={p} value={p}>
                    {paymentMethodLabels[p]}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-field">
              <label>Grupo (opcional)</label>
              <select
                value={form.group ?? ''}
                onChange={(e) => setForm((f) => ({ ...f, group: (e.target.value || null) as TransactionGroup | null }))}
              >
                <option value="">Nenhum</option>
                {transactionGroups.map((g) => (
                  <option key={g} value={g}>
                    {transactionGroupLabels[g]}
                  </option>
                ))}
              </select>
            </div>

            <div className="checkbox-field">
              <input
                type="checkbox"
                id="recurring"
                checked={form.isRecurring}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    isRecurring: e.target.checked,
                    recurrenceFrequency: e.target.checked ? f.recurrenceFrequency ?? 'MENSAL' : null,
                  }))
                }
              />
              <label htmlFor="recurring">Recorrente</label>
            </div>

            {form.isRecurring && (
              <div className="form-row">
                <div className="form-field">
                  <label>Frequência</label>
                  <select
                    value={form.recurrenceFrequency ?? 'MENSAL'}
                    onChange={(e) => setForm((f) => ({ ...f, recurrenceFrequency: e.target.value as RecurrenceFrequency }))}
                  >
                    {recurrenceFrequencies.map((r) => (
                      <option key={r} value={r}>
                        {recurrenceFrequencyLabels[r]}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-field">
                  <label>Data final (opcional)</label>
                  <input
                    type="date"
                    value={form.recurrenceEndDate ?? ''}
                    onChange={(e) => setForm((f) => ({ ...f, recurrenceEndDate: e.target.value || null }))}
                  />
                </div>
              </div>
            )}

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
