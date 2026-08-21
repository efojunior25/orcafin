import { useEffect, useMemo, useRef, useState } from 'react';
import * as transactionsApi from '../api/transactions';
import type { Transaction, TransactionInput, TransactionType, PaymentMethod, RecurrenceFrequency, TransactionGroup } from '../api/transactions';
import * as accountsApi from '../api/accounts';
import type { Account } from '../api/accounts';
import * as creditCardsApi from '../api/creditCards';
import type { CreditCard } from '../api/creditCards';
import * as prepaidCardsApi from '../api/prepaidCards';
import type { PrepaidCard } from '../api/prepaidCards';
import * as categoriesApi from '../api/categories';
import type { Category } from '../api/categories';
import * as aiApi from '../api/ai';
import { formatCurrency, formatDate, monthRange, todayISO, paymentMethodLabels, recurrenceFrequencyLabels, transactionGroupLabels, parseDecimal } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import './Accounts.css';
import './Transactions.css';

const paymentMethods: PaymentMethod[] = ['DINHEIRO', 'DEBITO', 'CREDITO', 'PIX', 'TED', 'DOC', 'TRANSFERENCIA'];
const recurrenceFrequencies: RecurrenceFrequency[] = ['DIARIA', 'SEMANAL', 'MENSAL', 'ANUAL'];
const transactionGroups: TransactionGroup[] = [
  'ASSINATURAS',
  'CONTAS_FIXAS_CASA',
  'COMPRAS_PONTUAIS',
  'DIVIDAS_PARCELAS',
  'ALIMENTACAO_CASA',
  'RESTAURANTES',
  'FATURA_CARTAO',
  'VALE_TRANSPORTE',
  'VALE_ALIMENTACAO_REFEICAO',
  'INVESTIMENTOS',
  'CAIXINHAS',
  'AJUSTES_ESTORNOS',
  'TARIFAS_JUROS',
  'OUTROS',
];

function emptyForm(accounts: Account[], categories: Category[]): TransactionInput {
  const despesaCategories = categories.filter((c) => c.type === 'DESPESA');
  return {
    accountId: accounts[0]?.id ?? null,
    creditCardId: null,
    prepaidCardId: null,
    categoryId: despesaCategories[0]?.id ?? null,
    destinationAccountId: null,
    type: 'DESPESA',
    group: null,
    amount: 0,
    description: '',
    payee: '',
    date: todayISO(),
    paymentMethod: 'DEBITO',
    isRecurring: false,
    recurrenceFrequency: null,
    recurrenceEndDate: null,
    installments: 1,
  };
}

export default function Transactions() {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [typeFilter, setTypeFilter] = useState<'ALL' | TransactionType>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [groupFilter, setGroupFilter] = useState<string>('ALL');
  const [accountFilter, setAccountFilter] = useState<string>('ALL');
  const [paymentMethodFilter, setPaymentMethodFilter] = useState<string>('ALL');
  const [searchText, setSearchText] = useState('');

  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [prepaidCards, setPrepaidCards] = useState<PrepaidCard[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Transaction | null>(null);
  const [form, setForm] = useState<TransactionInput>(emptyForm([], []));
  const [amountText, setAmountText] = useState('');
  const [saving, setSaving] = useState(false);

  const [aiText, setAiText] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [lastTranscription, setLastTranscription] = useState('');
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);

  async function loadAll() {
    setLoading(true);
    setError('');
    try {
      const { from, to } = monthRange(year, month);
      const [tx, acc, cc, pc, cat] = await Promise.all([
        transactionsApi.getTransactions(from, to),
        accountsApi.getAccounts(),
        creditCardsApi.getCreditCards(),
        prepaidCardsApi.getPrepaidCards(),
        categoriesApi.getCategories(),
      ]);
      setTransactions(tx);
      setAccounts(acc);
      setCreditCards(cc);
      setPrepaidCards(pc);
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
    const search = searchText.trim().toLowerCase();
    return transactions.filter((t) => {
      if (typeFilter !== 'ALL' && t.type !== typeFilter) return false;
      if (categoryFilter !== 'ALL' && t.categoryId !== categoryFilter) return false;
      if (groupFilter !== 'ALL' && t.group !== groupFilter) return false;
      if (paymentMethodFilter !== 'ALL' && t.paymentMethod !== paymentMethodFilter) return false;
      if (accountFilter !== 'ALL') {
        const [kind, id] = accountFilter.split(':');
        if (kind === 'acc' && t.accountId !== id) return false;
        if (kind === 'cc' && t.creditCardId !== id) return false;
        if (kind === 'pc' && t.prepaidCardId !== id) return false;
      }
      if (search && !t.description.toLowerCase().includes(search) && !(t.payee ?? '').toLowerCase().includes(search)) return false;
      return true;
    });
  }, [transactions, typeFilter, categoryFilter, groupFilter, accountFilter, paymentMethodFilter, searchText]);

  const categoriesForType = useMemo(
    () => categories.filter((c) => c.type === form.type),
    [categories, form.type]
  );

  function formatAmountForInput(value: number): string {
    return value ? String(value).replace('.', ',') : '';
  }

  function openCreate() {
    setEditing(null);
    setForm(emptyForm(accounts, categories));
    setAmountText('');
    setModalOpen(true);
  }

  function applyParsedResult(parsed: aiApi.ParsedTransaction) {
    setEditing(null);
    setForm({
      accountId: accounts[0]?.id ?? null,
      creditCardId: null,
      prepaidCardId: null,
      categoryId: parsed.categoryId,
      destinationAccountId: null,
      type: parsed.type,
      group: null,
      amount: parsed.amount,
      description: parsed.description,
      payee: '',
      date: parsed.date,
      paymentMethod: 'DEBITO',
      isRecurring: false,
      recurrenceFrequency: null,
      recurrenceEndDate: null,
    });
    setAmountText(formatAmountForInput(parsed.amount));
    setModalOpen(true);
  }

  async function handleAiParse() {
    if (!aiText.trim() || accounts.length === 0) return;
    setAiLoading(true);
    setError('');
    try {
      const parsed = await aiApi.parseTransactionText(aiText.trim());
      applyParsedResult(parsed);
      setAiText('');
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível interpretar o texto com a IA local.'));
    } finally {
      setAiLoading(false);
    }
  }

  async function startRecording() {
    if (accounts.length === 0) return;
    setError('');
    setLastTranscription('');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      audioChunksRef.current = [];

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) audioChunksRef.current.push(e.data);
      };

      recorder.onstop = async () => {
        stream.getTracks().forEach((track) => track.stop());
        const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        setAiLoading(true);
        try {
          const parsed = await aiApi.parseTransactionAudio(audioBlob);
          setLastTranscription(parsed.transcribedText ?? '');
          applyParsedResult(parsed);
        } catch (err) {
          setError(err instanceof Error ? err.message : 'Não foi possível transcrever o áudio.');
        } finally {
          setAiLoading(false);
        }
      };

      mediaRecorderRef.current = recorder;
      recorder.start();
      setIsRecording(true);
    } catch {
      setError('Não foi possível acessar o microfone. Verifique as permissões do navegador.');
    }
  }

  function stopRecording() {
    mediaRecorderRef.current?.stop();
    setIsRecording(false);
  }

  function openEdit(tx: Transaction) {
    setEditing(tx);
    setForm({
      accountId: tx.accountId,
      creditCardId: tx.creditCardId,
      prepaidCardId: tx.prepaidCardId,
      categoryId: tx.categoryId,
      destinationAccountId: tx.destinationAccountId,
      type: tx.type,
      group: tx.group,
      amount: tx.amount,
      description: tx.description,
      payee: tx.payee ?? '',
      date: tx.date,
      paymentMethod: tx.paymentMethod,
      installments: 1,
      isRecurring: tx.isRecurring,
      recurrenceFrequency: tx.recurrenceFrequency,
      recurrenceEndDate: tx.recurrenceEndDate,
    });
    setAmountText(formatAmountForInput(tx.amount));
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
        prepaidCardId: null,
        categoryId: null,
        destinationAccountId: otherAccount?.id ?? null,
      }));
      return;
    }
    const firstCat = categories.find((c) => c.type === type);
    const accountId = type === 'RECEITA' ? form.accountId ?? accounts[0]?.id ?? null : form.accountId;
    setForm((f) => ({
      ...f,
      type,
      accountId: type === 'RECEITA' ? accountId : f.accountId,
      creditCardId: type === 'RECEITA' ? null : f.creditCardId,
      prepaidCardId: type === 'RECEITA' ? null : f.prepaidCardId,
      categoryId: firstCat?.id ?? null,
      destinationAccountId: null,
    }));
  }

  function setPaymentSource(source: 'CONTA' | 'CARTAO' | 'PRE_PAGO') {
    if (source === 'CARTAO') {
      setForm((f) => ({ ...f, accountId: null, prepaidCardId: null, creditCardId: f.creditCardId ?? creditCards[0]?.id ?? null }));
    } else if (source === 'PRE_PAGO') {
      setForm((f) => ({ ...f, accountId: null, creditCardId: null, prepaidCardId: f.prepaidCardId ?? prepaidCards[0]?.id ?? null }));
    } else {
      setForm((f) => ({ ...f, creditCardId: null, prepaidCardId: null, accountId: f.accountId ?? accounts[0]?.id ?? null }));
    }
  }

  async function handleSave() {
    const isTransfer = form.type === 'TRANSFERENCIA';
    if ((!form.accountId && !form.creditCardId && !form.prepaidCardId) || !form.amount || !form.date) {
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
          <button
            className={`btn ${isRecording ? 'btn-danger' : 'btn-secondary'}`}
            onClick={isRecording ? stopRecording : startRecording}
            disabled={aiLoading || accounts.length === 0}
            title={isRecording ? 'Parar gravação' : 'Gravar áudio'}
          >
            {isRecording ? '⏹ Parar' : '🎤'}
          </button>
        </div>
        {lastTranscription && (
          <div className="ai-quick-entry-hint">Você disse: "{lastTranscription}"</div>
        )}
        <div className="ai-quick-entry-hint">
          Digite ou grave algo como "Pizza 59,90" e revise antes de salvar. Requer os containers Ollama/Whisper rodando.
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
        <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
          <option value="ALL">Todas as categorias</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <select value={groupFilter} onChange={(e) => setGroupFilter(e.target.value)}>
          <option value="ALL">Todos os grupos</option>
          {transactionGroups.map((g) => (
            <option key={g} value={g}>
              {transactionGroupLabels[g]}
            </option>
          ))}
        </select>
        <select value={accountFilter} onChange={(e) => setAccountFilter(e.target.value)}>
          <option value="ALL">Todas as contas</option>
          {accounts.map((a) => (
            <option key={`acc:${a.id}`} value={`acc:${a.id}`}>
              {a.name}
            </option>
          ))}
          {creditCards.map((c) => (
            <option key={`cc:${c.id}`} value={`cc:${c.id}`}>
              {c.name} (cartão)
            </option>
          ))}
          {prepaidCards.map((c) => (
            <option key={`pc:${c.id}`} value={`pc:${c.id}`}>
              {c.name} (pré-pago)
            </option>
          ))}
        </select>
        <select value={paymentMethodFilter} onChange={(e) => setPaymentMethodFilter(e.target.value)}>
          <option value="ALL">Todas as formas de pagamento</option>
          {paymentMethods.map((p) => (
            <option key={p} value={p}>
              {paymentMethodLabels[p]}
            </option>
          ))}
        </select>
        <input
          type="text"
          placeholder="Buscar por nome..."
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          className="filters-search"
        />
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
                    {tx.payee && <div className="recent-tx-meta">{tx.payee}</div>}
                    {tx.isRecurring && <span className="recurring-badge">↻ {recurrenceFrequencyLabels[tx.recurrenceFrequency ?? ''] ?? ''}</span>}
                    {tx.installmentTotal && tx.installmentTotal > 1 && (
                      <span className="recurring-badge">{tx.installmentNumber}/{tx.installmentTotal}</span>
                    )}
                  </td>
                  <td>{tx.type === 'TRANSFERENCIA' ? 'Transferência' : tx.categoryName}</td>
                  <td>{tx.group ? transactionGroupLabels[tx.group] : '—'}</td>
                  <td>
                    {tx.type === 'TRANSFERENCIA'
                      ? `${tx.accountName} → ${tx.destinationAccountName}`
                      : tx.creditCardName ?? tx.prepaidCardName ?? tx.accountName}
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
                  type="text"
                  inputMode="decimal"
                  placeholder="0,00"
                  value={amountText}
                  onChange={(e) => {
                    setAmountText(e.target.value);
                    setForm((f) => ({ ...f, amount: parseDecimal(e.target.value) }));
                  }}
                />
              </div>
              <div className="form-field">
                <label>Data</label>
                <input type="date" value={form.date} onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))} />
              </div>
            </div>

            <div className="form-row">
              <div className="form-field">
                <label>Descrição</label>
                <input
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                  placeholder="Ex: Supermercado do mês"
                />
              </div>
              <div className="form-field">
                <label>Favorecido (opcional)</label>
                <input
                  value={form.payee ?? ''}
                  onChange={(e) => setForm((f) => ({ ...f, payee: e.target.value }))}
                  placeholder="Ex: Nordestão, João Silva"
                />
              </div>
            </div>

            {form.type === 'DESPESA' && (
              <div className="type-toggle">
                <button
                  type="button"
                  className={!form.creditCardId && !form.prepaidCardId ? 'active-despesa' : ''}
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
                <button
                  type="button"
                  className={form.prepaidCardId ? 'active-despesa' : ''}
                  onClick={() => setPaymentSource('PRE_PAGO')}
                  disabled={prepaidCards.length === 0}
                >
                  Cartão Pré-pago
                </button>
              </div>
            )}

            <div className="form-row">
              {form.type === 'DESPESA' && form.creditCardId ? (
                <>
                  <div className="form-field">
                    <label>Cartão</label>
                    <select
                      value={form.creditCardId}
                      onChange={(e) => setForm((f) => ({ ...f, creditCardId: e.target.value }))}
                    >
                      {creditCards.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  {!editing && (
                    <div className="form-field">
                      <label>Parcelas</label>
                      <select
                        value={form.installments ?? 1}
                        onChange={(e) => setForm((f) => ({ ...f, installments: Number(e.target.value) }))}
                      >
                        {Array.from({ length: 48 }, (_, i) => i + 1).map((n) => (
                          <option key={n} value={n}>
                            {n === 1 ? 'À vista' : `${n}x de ${formatCurrency((form.amount || 0) / n)}`}
                          </option>
                        ))}
                      </select>
                    </div>
                  )}
                </>
              ) : form.type === 'DESPESA' && form.prepaidCardId ? (
                <div className="form-field">
                  <label>Cartão pré-pago</label>
                  <select
                    value={form.prepaidCardId}
                    onChange={(e) => setForm((f) => ({ ...f, prepaidCardId: e.target.value }))}
                  >
                    {prepaidCards.map((c) => (
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
                      const accountId = e.target.value;
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
                    onChange={(e) => setForm((f) => ({ ...f, destinationAccountId: e.target.value }))}
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
                    onChange={(e) => setForm((f) => ({ ...f, categoryId: e.target.value }))}
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
