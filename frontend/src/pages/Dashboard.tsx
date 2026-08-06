import { useEffect, useMemo, useState } from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import * as transactionsApi from '../api/transactions';
import type { Transaction } from '../api/transactions';
import * as accountsApi from '../api/accounts';
import type { Account } from '../api/accounts';
import * as budgetsApi from '../api/budgets';
import type { Budget } from '../api/budgets';
import { formatCurrency, formatDate, monthRange, accountTypeLabels } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import './Dashboard.css';
import './Budgets.css';

const COLORS = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#a855f7', '#14b8a6', '#eab308', '#ec4899'];

export default function Dashboard() {
  const now = new Date();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError('');
      try {
        const { from, to } = monthRange(now.getFullYear(), now.getMonth() + 1);
        const [tx, acc, bud] = await Promise.all([
          transactionsApi.getTransactions(from, to),
          accountsApi.getAccounts(),
          budgetsApi.getBudgets(),
        ]);
        setTransactions(tx);
        setAccounts(acc);
        setBudgets(bud);
      } catch (err) {
        setError(getErrorMessage(err, 'Não foi possível carregar o dashboard.'));
      } finally {
        setLoading(false);
      }
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const totalReceitas = useMemo(
    () => transactions.filter((t) => t.type === 'RECEITA').reduce((s, t) => s + t.amount, 0),
    [transactions]
  );
  const totalDespesas = useMemo(
    () => transactions.filter((t) => t.type === 'DESPESA').reduce((s, t) => s + t.amount, 0),
    [transactions]
  );
  const saldo = totalReceitas - totalDespesas;

  const despesasPorCategoria = useMemo(() => {
    const map = new Map<string, number>();
    transactions
      .filter((t) => t.type === 'DESPESA')
      .forEach((t) => {
        const name = t.categoryName ?? 'Outros';
        map.set(name, (map.get(name) ?? 0) + t.amount);
      });
    return Array.from(map.entries()).map(([name, value]) => ({ name, value }));
  }, [transactions]);

  const recentTransactions = useMemo(
    () =>
      [...transactions]
        .sort((a, b) => (a.date < b.date ? 1 : -1))
        .slice(0, 8),
    [transactions]
  );

  if (loading) {
    return <div className="empty-state">Carregando...</div>;
  }

  return (
    <div>
      <div className="page-header">
        <h1>Dashboard</h1>
        <div className="text-secondary">
          {now.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}
        </div>
      </div>

      {error && <div className="auth-error">{error}</div>}

      <div className="summary-grid">
        <div className="card summary-card">
          <div className="summary-label">Receitas do mês</div>
          <div className="summary-value text-accent">{formatCurrency(totalReceitas)}</div>
        </div>
        <div className="card summary-card">
          <div className="summary-label">Despesas do mês</div>
          <div className="summary-value text-danger">{formatCurrency(totalDespesas)}</div>
        </div>
        <div className="card summary-card">
          <div className="summary-label">Saldo do mês</div>
          <div className={`summary-value ${saldo >= 0 ? 'text-accent' : 'text-danger'}`}>{formatCurrency(saldo)}</div>
        </div>
      </div>

      {budgets.length > 0 && (
        <div className="card dashboard-section">
          <h2>Orçamento do mês</h2>
          <div className="budgets-list">
            {budgets.map((b) => (
              <div key={b.id}>
                <div className="budget-values">
                  <span>{b.categoryName}</span>
                  <span className={b.percentage >= 100 ? 'text-danger' : b.percentage >= 80 ? 'text-warning' : 'text-accent'}>
                    {formatCurrency(b.spentAmount)} / {formatCurrency(b.limitAmount)}
                  </span>
                </div>
                <div className="budget-bar-track">
                  <div
                    className={`budget-bar-fill ${b.percentage >= 100 ? 'over' : b.percentage >= 80 ? 'warn' : 'ok'}`}
                    style={{ width: `${Math.min(b.percentage, 100)}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="dashboard-columns">
        <div className="card dashboard-section">
          <h2>Despesas por categoria</h2>
          {despesasPorCategoria.length === 0 ? (
            <div className="empty-state">Sem despesas neste mês.</div>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={despesasPorCategoria}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  outerRadius={90}
                  label={(entry) => entry.name}
                >
                  {despesasPorCategoria.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="card dashboard-section">
          <h2>Contas</h2>
          {accounts.length === 0 ? (
            <div className="empty-state">Nenhuma conta cadastrada.</div>
          ) : (
            <div className="accounts-summary-list">
              {accounts.map((a) => (
                <div className="accounts-summary-row" key={a.id}>
                  <div>
                    <div>{a.name}</div>
                    <div className="recent-tx-meta">{accountTypeLabels[a.type] ?? a.type}</div>
                  </div>
                  <div className={a.balance < 0 ? 'text-danger' : 'text-accent'}>{formatCurrency(a.balance)}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="card dashboard-section">
        <h2>Últimas transações</h2>
        {recentTransactions.length === 0 ? (
          <div className="empty-state">Nenhuma transação neste mês.</div>
        ) : (
          <div>
            {recentTransactions.map((tx) => (
              <div className="recent-tx-row" key={tx.id}>
                <div>
                  <div>{tx.description}</div>
                  <div className="recent-tx-meta">
                    {formatDate(tx.date)} · {tx.type === 'TRANSFERENCIA' ? `${tx.accountName} → ${tx.destinationAccountName}` : `${tx.categoryName} · ${tx.accountName}`}
                  </div>
                </div>
                <div
                  className={
                    tx.type === 'RECEITA'
                      ? 'tx-amount-receita'
                      : tx.type === 'DESPESA'
                        ? 'tx-amount-despesa'
                        : 'tx-amount-transferencia'
                  }
                >
                  {tx.type === 'RECEITA' ? '+' : tx.type === 'DESPESA' ? '-' : '⇄'} {formatCurrency(tx.amount)}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
