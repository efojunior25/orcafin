import { useEffect, useState } from 'react';
import * as authApi from '../api/auth';
import type { LoginAudit } from '../api/auth';
import { formatDateTime } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import './Accounts.css';

export default function LoginHistory() {
  const [history, setHistory] = useState<LoginAudit[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError('');
      try {
        const data = await authApi.getLoginHistory();
        setHistory(data);
      } catch (err) {
        setError(getErrorMessage(err, 'Não foi possível carregar o histórico de acessos.'));
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  return (
    <div>
      <div className="page-header">
        <h1>Histórico de Acessos</h1>
      </div>

      {error && <div className="auth-error">{error}</div>}

      {loading ? (
        <div className="empty-state">Carregando...</div>
      ) : history.length === 0 ? (
        <div className="empty-state">Nenhum acesso registrado ainda.</div>
      ) : (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Data/Hora</th>
                <th>Status</th>
                <th>IP</th>
                <th>Dispositivo</th>
              </tr>
            </thead>
            <tbody>
              {history.map((h) => (
                <tr key={h.id}>
                  <td>{formatDateTime(h.createdAt)}</td>
                  <td className={h.success ? 'text-accent' : 'text-danger'}>
                    {h.success ? 'Sucesso' : 'Falha'}
                  </td>
                  <td>{h.ip}</td>
                  <td style={{ maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {h.userAgent ?? '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
