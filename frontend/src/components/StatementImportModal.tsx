import { useState } from 'react';
import * as statementsApi from '../api/statements';
import type { Suggestion } from '../api/statements';
import { formatCurrency, formatDate } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import './StatementImportModal.css';

const typeLabels: Record<string, string> = {
  ADD: 'Adicionar',
  REMOVE: 'Remover',
  FIX_DATE: 'Corrigir data',
  MOVE_ACCOUNT: 'Mover de conta',
};

interface Props {
  accountId?: string;
  creditCardId?: string;
  onClose: () => void;
  onApplied: () => void;
}

export default function StatementImportModal({ accountId, creditCardId, onClose, onApplied }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [checked, setChecked] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleUpload() {
    if (!file) return;
    setLoading(true);
    setError('');
    try {
      const result = await statementsApi.importStatement(file, { accountId, creditCardId });
      setSessionId(result.sessionId);
      setSuggestions(result.suggestions);
      setChecked(new Set(result.suggestions.map((s) => s.id)));
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível ler esse arquivo. Tente um PDF, CSV ou OFX do extrato/fatura.'));
    } finally {
      setLoading(false);
    }
  }

  function toggle(id: string) {
    setChecked((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function handleApply() {
    if (!sessionId) return;
    setLoading(true);
    setError('');
    try {
      await statementsApi.applySuggestions(sessionId, Array.from(checked));
      onApplied();
      onClose();
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível aplicar as correções.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="card modal-card wide" onClick={(e) => e.stopPropagation()}>
        <h2>Importar extrato</h2>

        {error && <div className="auth-error">{error}</div>}

        {!sessionId ? (
          <>
            <p className="text-secondary">
              Suba o extrato ou fatura (PDF, CSV ou OFX). O app compara com o que já está lançado e sugere
              correções — nada é aplicado sem sua revisão.
            </p>
            <div className="form-field">
              <input
                type="file"
                accept=".pdf,.csv,.ofx,.qfx"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            </div>
            <div className="modal-actions">
              <button className="btn btn-secondary" onClick={onClose}>
                Cancelar
              </button>
              <button className="btn btn-primary" onClick={handleUpload} disabled={!file || loading}>
                {loading ? 'Lendo arquivo...' : 'Analisar'}
              </button>
            </div>
          </>
        ) : (
          <>
            {suggestions.length === 0 ? (
              <div className="empty-state">Tudo certo — não achei diferenças entre o extrato e o app.</div>
            ) : (
              <div className="suggestion-list">
                {suggestions.map((s) => (
                  <label key={s.id} className="suggestion-item">
                    <input type="checkbox" checked={checked.has(s.id)} onChange={() => toggle(s.id)} />
                    <div className="suggestion-body">
                      <div className="suggestion-top">
                        <span className={`suggestion-badge suggestion-badge-${s.type.toLowerCase()}`}>
                          {typeLabels[s.type] ?? s.type}
                        </span>
                        <span className="suggestion-amount">
                          {s.transactionType === 'RECEITA' ? '+' : '-'} {formatCurrency(s.amount)}
                        </span>
                      </div>
                      <div className="suggestion-desc">{s.description}</div>
                      <div className="suggestion-meta">
                        {s.type === 'FIX_DATE' && (
                          <>Data no app: {formatDate(s.currentDate!)} → extrato: {formatDate(s.date)}</>
                        )}
                        {s.type === 'MOVE_ACCOUNT' && (
                          <>Está em "{s.currentAccountName}" → mover pra cá ({formatDate(s.date)})</>
                        )}
                        {(s.type === 'ADD' || s.type === 'REMOVE') && <>{formatDate(s.date)}</>}
                      </div>
                      <div className="suggestion-reason">{s.reason}</div>
                    </div>
                  </label>
                ))}
              </div>
            )}
            <div className="modal-actions">
              <button className="btn btn-secondary" onClick={onClose}>
                Fechar
              </button>
              {suggestions.length > 0 && (
                <button className="btn btn-primary" onClick={handleApply} disabled={loading || checked.size === 0}>
                  {loading ? 'Aplicando...' : `Aplicar selecionadas (${checked.size})`}
                </button>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
