import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import * as authApi from '../api/auth';
import { getErrorMessage } from '../utils/errors';
import './Auth.css';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    if (!email) {
      setError('Digite seu e-mail.');
      return;
    }
    setLoading(true);
    try {
      await authApi.forgotPassword(email);
      setSent(true);
    } catch (err) {
      setError(getErrorMessage(err, 'Não foi possível processar sua solicitação.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <div className="auth-logo">
          Orça<span>Fin</span>
        </div>
        <div className="auth-subtitle">Recuperar senha</div>
        {error && <div className="auth-error">{error}</div>}

        {sent ? (
          <div className="auth-footer" style={{ textAlign: 'center' }}>
            Se esse e-mail estiver cadastrado, você vai receber um link pra redefinir sua senha em instantes.
          </div>
        ) : (
          <>
            <div className="auth-field">
              <label htmlFor="email">E-mail</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="voce@email.com"
                autoComplete="email"
              />
            </div>
            <button className="btn btn-primary auth-submit" type="submit" disabled={loading}>
              {loading ? 'Enviando...' : 'Enviar link de recuperação'}
            </button>
          </>
        )}

        <div className="auth-footer">
          <Link to="/login">Voltar para o login</Link>
        </div>
      </form>
    </div>
  );
}
